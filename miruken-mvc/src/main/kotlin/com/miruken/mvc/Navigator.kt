package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.context.Context
import com.miruken.mvc.view.ViewingRegion

class Navigator(mainRegion: ViewingRegion) : CompositeHandler() {

    init {
        addHandlers(mainRegion)
    }

    @Handles
    fun <C: Controller> navigate(
            navigation: Navigation<C>,
            composer:   Handling
    ): Any? {
        val context = composer.resolve<Context>()
            ?: error("A context is required for controller navigation")

        val style            = navigation.style
        val initiator        = context.xselfOrChild.resolve<Navigation<*>>()
        val initiatorContext = initiator?.controller?.context
        var parentContext    = context

        if (initiator != null && style == NavigationStyle.NEXT) {
            parentContext = initiatorContext?.parent ?: return null
        }

        var controller: C? = null
        val childContext = parentContext.createChild()

        try {
            @Suppress("UNCHECKED_CAST")
            controller = getController(
                    navigation.controllerKey,
                    childContext)?.let { it as C }
                    ?: return null
        } catch(e: Throwable) {
            return null
        } finally {
            if (controller == null) {
                childContext.end()
            }
        }

        childContext.addHandlers(navigation)

        if (initiator != null && style == NavigationStyle.NEXT) {
            navigation.back = navigation
            initiatorContext?.end()
        }

        try {
            // Propagate composer options
            val io = childContext.xself + composer
            bindIO(io, controller!!)
            try {
                navigation.invokeOn(controller)
            } finally {
                bindIO(null, controller)
            }
        } catch(e: Throwable) {
            childContext.end()
        }

        return true
    }

    @Handles
    fun <C: Controller> navigate(
            goBack:   GoBack,
            composer: Handling
    ): Any? = composer.resolve<Navigation<*>>()?.back?.also {
        composer.handle(it) success {
            goBack.setResult(it.clearResult())
            return true
        }
    }

    private fun bindIO(io: Handling?, controller: Controller) {
        controller._io = io?.let {
            Navigation.GLOBAL_PREPARE.foldRight(it) {
                filter, pipe -> filter(pipe)
            }
        }
    }

    private fun getController(
            controllerKey: Any,
            context:       Context
    ): Controller? {
        val controller = context.resolve(controllerKey) as? Controller
                ?: return null
        context.contextEnded += { _ -> controller.release() }
        controller.context = context
        return controller
    }
}