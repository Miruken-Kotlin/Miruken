package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.concurrent.ChildCancelMode
import com.miruken.concurrent.Promise
import com.miruken.context.Context
import com.miruken.context.Scoped
import com.miruken.graph.TraversingAxis
import com.miruken.mvc.option.NavigationOptions
import com.miruken.mvc.option.RegionOptions
import com.miruken.mvc.option.navigationOptions
import com.miruken.mvc.option.noBack
import com.miruken.mvc.view.ViewingRegion
import com.miruken.typeOf

class Navigator(mainRegion: ViewingRegion) : CompositeHandler() {

    init { addHandlers(mainRegion) }

    @Handles
    fun <C: Controller> navigate(
            navigation: Navigation<C>,
            context:    Context,
            composer:   Handling
    ): Promise<Context>? {
        val style     = navigation.style
        var initiator = context.xself.resolve<Navigation<*>>()
        var parent    = context

        if (initiator != null) {
            if (initiator.style == NavigationStyle.PARTIAL &&
                    navigation.style != NavigationStyle.PARTIAL) {
                val nearest = findNearest(parent) ?: error(
                        "Navigation from a partial requires a parent")
                parent    = nearest.first
                initiator = nearest.second
            }

            if ((style != NavigationStyle.PUSH)) {
                parent = parent.parent ?: error(
                        "Navigation seems to be in a bad state")
                navigation.viewLayer = initiator.viewLayer
            }
        }

        var controller: C? = null
        val child = parent.createChild().let {
            if (style == NavigationStyle.PUSH) it.createChild() else it
        }

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

        with(navigation) {
            noBack = options?.noBack == true
            if (!noBack && back == null && initiator != null &&
                    style == NavigationStyle.NEXT) {
                back = initiator
            }
        }

        bindIO(child, controller!!, style, options, composer)

        child.addHandlers(GenericWrapper(navigation, typeOf<Navigation<*>>()))

        return Promise(ChildCancelMode.ANY) { resolve, reject ->
            try {
                child.contextEnding += { (ctx, reason) ->
                    if (reason !is Navigation<*>) {
                        resolve(ctx)
                    }
                }
                if (style == NavigationStyle.PUSH) {
                    child.parent!!.childContextEnded += { (ctx, reason) ->
                        if (reason !is Navigation<*>) {
                            ctx.parent?.end(reason)
                            resolve(ctx)
                        }
                    }
                }
                if (!navigation.invokeOn(controller) { args ->
                        child.resolveArgs(*args)?.also {
                            if (style != NavigationStyle.PUSH) {
                                initiator?.context?.end(initiator)
                            }
                        }}) {
                    reject(IllegalStateException(
                            "Navigation could not be performed.  The most likely cause is missing dependencies."))
                    child.end()
                }
            } catch (t: Throwable) {
                reject(t)
                child.end()
            } finally {
                bindIO(null, controller, style)
            }
        }
    }

    @Handles
    @Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
    fun navigate(goBack: Navigation.GoBack, composer: Handling): Promise<Context>? =
            composer.resolve<Navigation<*>>()?.let {
                val nav = when {
                    it.noBack -> return@let null
                    it.style == NavigationStyle.PARTIAL ->
                        it.context?.let { ctx ->
                            findNearest(ctx)?.second
                        }
                    else -> it
                }
                when {
                    nav == null -> null
                    nav.back != null -> composer.noBack.commandAsync(nav.back!!)
                    nav.style == NavigationStyle.PUSH ->
                        nav.context?.let { ctx ->
                            ctx.end()
                            Promise.resolve(ctx)
                        }
                    else -> null
                } as? Promise<Context>
            }

    private fun bindIO(
            io:         Handling?,
            controller: Controller,
            style:      NavigationStyle,
            options:    NavigationOptions? = null,
            composer:   Handling? = null
    ) {
        controller._io = (io ?: controller.context)?.let {
            Navigation.GLOBAL_PREPARE.foldRight(it) { filter, pipe -> filter(pipe) }
        }?.let {
            if (composer != null) {
                var navOptions = options
                if (style == NavigationStyle.PUSH) {
                    if (navOptions == null)
                        navOptions = NavigationOptions()
                    navOptions.region = (navOptions.region
                            ?: RegionOptions()).apply { push = true }
                }
                if (navOptions != null) {
                    return@let it.stop.navigationOptions(navOptions)
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