package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.context.ContextualHandler
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.policy.PolicyOwner
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingRegion
import com.miruken.mvc.view.addRegion
import com.miruken.mvc.view.show

abstract class Controller : ContextualHandler(),
        PolicyOwner<ControllerPolicy> {

    @Suppress("PropertyName")
    internal var _io: Handling? = null

    override val policy by lazy { ControllerPolicy(this) }

    // Context

    protected val io get() =
        _io ?: context ?: error(
            "${this::class.qualifiedName} is not bound to a context")

    protected fun endContext() = context?.end()

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
    ) = context?.addRegion(region)?.apply {
            init?.invoke(region(this))
        }

    // Navigate

    protected inline fun <reified C: Controller> next(
            noinline action: C.() -> Any?
    ) = io.next(action)

    protected inline fun <reified C: Controller> next(
            handler:         Handling,
            noinline action: C.() -> Any?
    ) = handler.next(action)

    protected inline fun <reified C: Controller> push(
            noinline action: C.() -> Any?
    ) = io.push(action)

    protected inline fun <reified C: Controller> push(
            handler:         Handling,
            noinline action: C.() -> Any?
    ) = handler.next(action)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            noinline action: C.() -> Any?
    ) = io.navigate(action, style)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            handler:         Handling,
            noinline action: C.() -> Any?
    ) = handler.navigate(action, style)

    protected fun goBack() = io.goBack()

    protected fun goBack(handler: Handling) =
        handler.goBack()

    override fun cleanUp() {
        policy.release()
        context = null
        _io     = null
    }
}