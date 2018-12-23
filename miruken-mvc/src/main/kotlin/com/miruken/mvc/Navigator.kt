package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.graph.TraversingAxis
import com.miruken.mvc.option.NavigationOptions
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.noBack
import com.miruken.mvc.option.regionOptions
import com.miruken.mvc.view.ViewingRegion
import com.miruken.typeOf

class Navigator(mainRegion: ViewingRegion) : CompositeHandler() {

    init { addHandlers(mainRegion) }

    @Handles
    fun <C: Controller> navigate(
            navigation: Navigation<C>,
            composer:   Handling
    ): Any? {
        val from = navigation.from ?: composer.resolve()
            ?: error("Unable to determine navigation from")

        val style     = navigation.style
        val push      = style == NavigationStyle.PUSH ||
                        style == NavigationStyle.FORK
        var initiator = from.xself.resolve<Navigation<*>>()
        var parent    = initiator?.join ?: from

        if (initiator != null) {
            if (initiator.style == NavigationStyle.PARTIAL &&
                    navigation.style != NavigationStyle.PARTIAL) {
                val nearest = findNearest(parent) ?:
                        error("Navigation join a partial requires a parent")
                parent    = nearest.first
                initiator = nearest.second
            }

            if (!push) {
                parent = parent.parent ?: error(
                        "Navigation seems to be in a bad state")
            }
        }

        if (push) {
            NavigatingAware(parent.xselfOrDescendant.notify)
                    .navigating(navigation)
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
        val options = composer.getOptions(NavigationOptions())

        if (initiator != null && navigation.back == null &&
                options?.noBack != true && style == NavigationStyle.NEXT) {
            navigation.back = initiator
        }

        bindIO(child, controller!!, push, options, composer)

        child.addHandlers(GenericWrapper(navigation, typeOf<Navigation<*>>()))

        try {
            if (!navigation.invokeOn(controller)) {
                child.end()
                return null
            }
            if (!push || initiator?.join != null) {
                initiator?.controller?.also {
                    it.context?.also { ctx ->
                        val reason = initiator.takeIf {
                            initiator.join == null
                        } ?: it
                        ctx.end(reason)
                    }
                }
            }
        } catch(e: Throwable) {
            child.end()
            throw e
        } finally {
            bindIO(null, controller, push, null, null)
        }
        return true
    }

    @Handles
    @Suppress("UNUSED_PARAMETER")
    fun navigate(goBack: Navigation.GoBack, composer: Handling) =
            composer.resolve<Navigation<*>>()?.let {
                val nav = when {
                    it.style == NavigationStyle.PARTIAL ->
                        it.controller?.context?.let { ctx ->
                            findNearest(ctx)?.second
                        }
                    else -> it
                }
                when {
                    nav == null -> null
                    nav.back?.style == NavigationStyle.FORK ->
                        nav.back?.from?.noBack?.handle(nav.back!!)
                    nav.back != null -> composer.noBack.handle(nav.back!!)
                    nav.style == NavigationStyle.PUSH ||
                    nav.style == NavigationStyle.FORK ->
                        nav.controller?.let { controller ->
                            controller.endContext()
                            HandleResult.HANDLED
                        }
                    else -> null
                }
            }

    private fun bindIO(
            io:         Handling?,
            controller: Controller,
            pushLayer:  Boolean,
            options:    NavigationOptions?,
            composer:   Handling?
    ) {
        controller._io = (io ?: controller.context)?.let {
            GLOBAL_PREPARE.foldRight(it) {
                filter, pipe -> filter(pipe)
            }
        }?.let {
            if (composer != null) {
                var navOptions = options
                if (pushLayer) {
                    if (navOptions == null)
                        navOptions = NavigationOptions()
                    navOptions.region = (navOptions.region
                            ?: RegionOptions()).apply { push = true }
                }
                if (navOptions != null) {
                    return@let it.stop.regionOptions(navOptions)
                }
            }
            it
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