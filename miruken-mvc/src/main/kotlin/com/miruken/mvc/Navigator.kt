package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.container.Container
import com.miruken.context.Context
import com.miruken.error.Errors
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.view.ViewRegion
import com.miruken.protocol.proxy

class Navigator(mainRegion: ViewRegion) : CompositeHandler(), Navigate {

    init {
        addHandlers(mainRegion)
    }

    override fun navigate(
            controllerKey: Any,
            style:         NavigationStyle,
            action:        (Controller) -> Unit
    ) {
        var composer = COMPOSER
        val context  = composer?.resolve<Context>() ?:
        throw IllegalStateException(
                "A context is required for controller navigation")

        val initiator = composer.resolve<Controller>()

        val ctx = when (style) {
            NavigationStyle.PUSH -> context.createChild()
            else -> context
        }

        val controller = try {
            resolveController(controllerKey, ctx)
        } catch (e: Throwable) {
            if (style == NavigationStyle.PUSH) ctx.end()
            throw e
        }

        initiator?.takeUnless {
            it == controller || it.context == ctx }
                ?.dependsOn(controller)

        try {
            if (style == NavigationStyle.PUSH) {
                composer = composer.pushLayer
            } else {
                controller._lastAction  = { Navigate(it).next(action) }
                controller._retryAction = initiator?._lastAction
            }

            // Propagate composer options
            val io  = when {
                context === ctx -> composer
                else -> ctx.xself + composer
            }
            bindIO(io, controller)

            try {
                action(controller)
            } finally {
                bindIO(null, controller)
                initiator?.takeUnless {
                    it == controller || it.context != ctx }?.also {
                    it.release()
                    it.context = null
                }
            }
        } catch (e: Throwable) {
            if (style == NavigationStyle.PUSH) {
                ctx.end()
            } else if (initiator?.context == ctx) {
                controller.dependsOn(initiator)
            }
            controller._io?.run { Errors(this).handleException(e) }
        }
    }

    override fun goBack() {
        val composer   = COMPOSER
        val controller = composer?.resolve<Controller>()
        controller?._retryAction?.invoke(composer)
    }

    private fun bindIO(io: Handling?, controller: Controller) {
        controller._io = io ?: controller.context?.let { h: Handling ->
            Controller.GLOBAL_PREPARE.foldRight(h, { filter, pipe ->
                filter(pipe)
            })
        }
    }

    private fun resolveController(
            controllerKey: Any,
            context:       Context
    ): Controller {
        val controller = context.proxy<Container>()
                .resolve(controllerKey) as? Controller
                ?: throw IllegalArgumentException(
                    "Controller '$controllerKey' could not be resolved")
        context.contextEnded += { _ -> controller.release() }
        controller.policy.autoRelease()
        controller.context = context
        return controller
    }
}