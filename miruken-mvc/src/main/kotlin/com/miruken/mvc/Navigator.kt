package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.graph.TraversingAxis
import com.miruken.mvc.option.LayerOptions
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.regionOptions
import com.miruken.mvc.view.ViewingRegion
import com.miruken.typeOf

class Navigator(mainRegion: ViewingRegion) : CompositeHandler() {

    init { addHandlers(mainRegion) }

    @Handles
    fun <C: Controller> navigate(
            navigation: Navigation<C>,
            context:    Context,
            composer:   Handling
    ): Any? {
        val style     = navigation.style
        var initiator = context.xself.resolve<Navigation<*>>()
        var parent    = context

        if (initiator != null) {
            if (initiator.style == NavigationStyle.PARTIAL &&
                    navigation.style != NavigationStyle.PARTIAL) {
                val nearest = findNearest(context) ?:
                        error("Navigation from a partial requires a parent")
                parent    = nearest.first
                initiator = nearest.second
            }

            if (style != NavigationStyle.PUSH) {
                parent = parent.parent ?: error(
                        "Navigation seems to be in a bad state")
            }
        }

        var controller: C? = null
        val child = parent.createChild()

        try {
            @Suppress("UNCHECKED_CAST")
            controller = ((child.xself + composer)
                    .infer.resolve(navigation.controllerKey) {
                require(Qualifier<Scoped>())
            } as? C)?.also { it.context = child } ?: return null
        } catch(e: Throwable) {
            return null
        } finally {
            if (controller == null) {
                child.end()
            }
        }

        if (style == NavigationStyle.NEXT) {
            navigation.back = initiator
        }

        bindIO(child, controller!!, style, composer)

        child.addHandlers(GenericWrapper(
                navigation, typeOf<Navigation<*>>()))
        try {
            navigation.invokeOn(controller)
            if (style != NavigationStyle.PUSH) {
                initiator?.controller?.context?.end(initiator)
            }
        } catch(e: Throwable) {
            child.end()
            throw e
        } finally {
            bindIO(null, controller, style, null)
        }
        return true
    }

    @Handles
    @Suppress("UNUSED_PARAMETER")
    fun navigate(goBack: Navigation.GoBack, composer: Handling) =
            composer.resolve<Navigation<*>>()?.back?.let {
                composer.handle(it)
            }

    private fun bindIO(
            io:         Handling?,
            controller: Controller,
            style:      NavigationStyle,
            composer:   Handling?
    ) {
        controller._io = (io ?: controller.context)?.let {
            Navigation.GLOBAL_PREPARE.foldRight(it) {
                filter, pipe -> filter(pipe)
            }
        }?.let {
            if (composer != null) {
                val options    = RegionOptions()
                var hasOptions = composer.handle(options).handled
                if (style == NavigationStyle.PUSH) {
                    options.layer = (options.layer ?: LayerOptions()).apply {
                        push = true
                    }
                    hasOptions = true
                }
                if (hasOptions) it.stop.regionOptions(options) else it
            } else {
                it
            }
        }
    }

    private fun findNearest(
            context: Context
    ): Pair<Context, Navigation<*>>? {
        var nearest: Pair<Context, Navigation<*>>? = null
        context.traverse(TraversingAxis.ANCESTOR) { node ->
            val ctx = node as Context
            val nav = ctx.xself.resolve<Navigation<*>>()
            if (nav == null || nav.controller?.context != ctx ||
                    nav.style == NavigationStyle.PARTIAL) {
                false
            } else {
                nearest = ctx to nav
                true
            }
        }
        return nearest
    }
}