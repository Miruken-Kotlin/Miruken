package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.context.Context
import com.miruken.mvc.view.ViewingRegion
import com.miruken.typeOf

class Navigator(mainRegion: ViewingRegion) : CompositeHandler() {

    init { addHandlers(mainRegion) }

    @Handles
    fun <C: Controller> navigate(
            navigation: Navigation<C>,
            initiator:  Navigation<*>?,
            context:    Context,
            composer:   Handling
    ): Any? {
        val style            = navigation.style
        val initiatorContext = initiator?.controller?.context
        var parentContext    = context

        if (initiator != null && style != NavigationStyle.PUSH) {
            parentContext = initiatorContext?.parent ?: return null
        }

        var controller: C? = null
        val childContext = parentContext.createChild()

        try {
            @Suppress("UNCHECKED_CAST")
            controller = (childContext.infer.resolve(
                    navigation.controllerKey) as? C)?.also {
                it.context = childContext
            } ?: return null
        } catch(e: Throwable) {
            return null
        } finally {
            if (controller == null) {
                childContext.end()
            }
        }

        // TODO: typeOf() does not support type variables yet
        // TODO:   i.e. Navigation<C>  C => TypeVariable
        // TODO: so we use Navigation<*> instead

        val io = (childContext.xself + composer).let {
            if (style == NavigationStyle.PARTIAL) {
                it.provide(navigation as Navigation<*>)
            } else {
                childContext.addHandlers(GenericWrapper(
                        navigation, typeOf<Navigation<*>>()))
                it
            }
        }

        if (initiator != null && style == NavigationStyle.NEXT) {
            navigation.back = navigation
            initiatorContext?.end()
        }

        // Propagate options (i.e. animation)
        bindIO(io, controller!!)

        try {
            navigation.invokeOn(controller)
        } catch(e: Throwable) {
            childContext.end()
            throw e
        } finally {
            bindIO(null, controller)
        }
        return true
    }

    @Handles
    @Suppress("UNUSED_PARAMETER")
    fun navigate(goBack: GoBack, composer: Handling) =
            composer.resolve<Navigation<*>>()?.back?.let {
                composer.handle(it)
            }

    private fun bindIO(io: Handling?, controller: Controller) {
        controller._io = io?.let {
            Navigation.GLOBAL_PREPARE.foldRight(it) {
                filter, pipe -> filter(pipe)
            }
        }
    }
}