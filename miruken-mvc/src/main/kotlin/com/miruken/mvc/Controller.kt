package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.context.ContextualHandler
import com.miruken.context.requireContext
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingRegion
import com.miruken.mvc.view.addRegion
import com.miruken.mvc.view.show

abstract class Controller : ContextualHandler() {

    @Suppress("PropertyName")
    internal var _io: Handling? = null

    // Context

    protected val io get() =
        _io ?: context ?: error(
            "${this::class.qualifiedName} is not bound to a context")

    fun endContext() = context?.end(this)

    // Render

    protected inline fun <reified V: Viewing> show(
            noinline init: (V.() -> Unit)? = null
    ) = region(io).show(init)

    protected inline fun <reified V: Viewing> show(
            handler:       Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler).show(init)

    protected fun show(view: Viewing) =
            view.display(region(io))

    protected fun show(handler: Handling, view: Viewing) =
            view.display(region(handler))

    protected inline fun <reified V: Viewing> overlay(
            noinline init: (V.() -> Unit)? = null
    ) = region(io.pushLayer).show(init)

    protected inline fun <reified V: Viewing> overlay(
            handler: Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler.pushLayer).show(init)

    protected fun region(handler: Handling) =
            ViewingRegion(handler)

    protected fun addRegion(
            region: ViewingRegion,
            init:   (ViewingRegion.() -> Unit)? = null
    ) = requireContext().addRegion(region).apply {
            init?.invoke(region(this))
        }

    // Navigate

    protected inline fun <reified C: Controller> next(
            noinline action: C.() -> Unit
    ) = requireContext().next(action)

    protected inline fun <reified C: Controller> next(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.next(action)

    protected inline fun <reified C: Controller> push(
            noinline action: C.() -> Unit
    ) = requireContext().push(action)

    protected inline fun <reified C: Controller> push(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.push(action)

    protected inline fun <reified C: Controller> partial(
            noinline action: C.() -> Unit
    ) = requireContext().partial(action)

    protected inline fun <reified C: Controller> partial(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.partial(action)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            noinline action: C.() -> Unit
    ) = requireContext().navigate(action, style)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.navigate(action, style)

    protected fun goBack() = requireContext().goBack()

    protected fun goBack(handler: Handling) =
        handler.goBack()

    override fun cleanUp() {
        context = null
        _io     = null
    }
}