package com.miruken.mvc

import com.miruken.callback.COMPOSER
import com.miruken.callback.Handling
import com.miruken.callback.resolve
import com.miruken.context.Context
import com.miruken.context.ContextImpl
import com.miruken.context.ContextualHandler
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.policy.PolicyOwner
import com.miruken.mvc.view.View
import com.miruken.mvc.view.ViewRegion
import com.miruken.mvc.view.addRegion
import com.miruken.mvc.view.show
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("PropertyName")
open class Controller : ContextualHandler<Context>(),
        PolicyOwner<ControllerPolicy>, AutoCloseable {

    internal var _io:          Handling? = null
    internal var _lastAction:  ((Handling) -> Any?)? = null
    internal var _retryAction: ((Handling) -> Any?)? = null
    private  val _closed =     AtomicBoolean()

    @Suppress("LeakingThis")
    override val policy = ControllerPolicy(this)

    // Render

    protected inline fun <reified V: View> show(
            noinline init: (V.() -> Unit)? = null
    ) = region(io).show(init)

    protected inline fun <reified V: View> show(
            handler:       Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler).show(init)

    protected fun show(view: View) =
            view.display(region(io))

    protected fun show(handler: Handling, view: View) =
            view.display(region(handler))

    protected inline fun <reified V: View> overlay(
            noinline init: (V.() -> Unit)? = null
    ) = region(io.pushLayer).show(init)

    protected inline fun <reified V: View> overlay(
            handler: Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler.pushLayer).show(init)

    protected fun region(handler: Handling) =
            ViewRegion(handler)

    protected fun addRegion(
        region: ViewRegion,
        init:   (ViewRegion.() -> Unit)? = null
    ) = context?.addRegion(region)?.apply {
            init?.invoke(region(this))
        }

    // Navigate

    protected inline fun <reified C: Controller> next(
            noinline action: C.() -> Unit
    ) = io.next(action)

    protected inline fun <reified C: Controller> next(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.next(action)

    protected inline fun <reified C: Controller> push(
            noinline action: C.() -> Unit
    ) = io.push(action)

    protected inline fun <reified C: Controller> push(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.next(action)

    protected inline fun <reified C: Controller> navigate(
            style:  NavigationStyle,
            noinline action: C.() -> Unit
    ) = io.navigate(style, action)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.navigate(style, action)

    protected fun goBack() = io.goBack()

    protected fun goBack(handler: Handling) =
        handler.goBack()

    // Context

    protected val io get() =
        _io ?: context ?: ContextImpl()

    protected fun endContext() = context?.end()

    protected fun endCallingContext() {
        COMPOSER?.resolve<Context>()
                ?.takeUnless { it == context }
                ?.end()
    }

    override fun close() {
        error("")
        if (_closed.compareAndSet(false, true)) {
            policy.release()
            context      = null
            _io          = null
            _lastAction  = null
            _retryAction = null
        }
    }

    companion object {
        val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
        val GLOBAL_EXECUTE = mutableListOf<(Handling) -> Handling>()
    }
}