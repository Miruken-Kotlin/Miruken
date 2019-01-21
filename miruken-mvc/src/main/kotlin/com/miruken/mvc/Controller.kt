package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.context.*
import com.miruken.event.Event
import com.miruken.mvc.option.noBack
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingRegion
import com.miruken.mvc.view.addRegion
import com.miruken.mvc.view.show
import java.util.concurrent.atomic.AtomicBoolean

abstract class Controller : Contextual, AutoCloseable {
    private var _context: Context? = null
    private val _closed = AtomicBoolean()

    @Suppress("PropertyName")
    internal var _io: Handling? = null

    // Context

    protected val io get() =
        _io ?: context ?: error(
            "${this::class.qualifiedName} is not bound to a context")

    override var context: Context?
        get() = _context
        set(value) {
            if (_context == value) return
            val changingEvent = ContextChangingEvent(this, _context, value)
            contextChanging {
                ContextChangingEvent(this, _context, value)
            }
            _context?.removeHandlers(this)
            val oldContext = _context
            _context = changingEvent.newContext
            _context?.insertHandlers(0, this)
            contextChanged {
                ContextChangedEvent(this, oldContext, _context)
            }
        }

    override val contextChanging = Event<ContextChangingEvent>()
    override val contextChanged  = Event<ContextChangedEvent>()

    fun endContext() = context?.end(this)

    fun unwindContext() = context?.unwind(this)

    fun track(promise: Promise<*>) {
        context?.track(promise)
    }

    fun dispose(closeable: AutoCloseable) {
        context?.dispose(closeable)
    }

    val async get() = requireContext().async

    // Render

    protected inline fun <reified V: Viewing> show(
            noinline init: (V.() -> Unit)? = null
    ) = region(io).show(init)

    protected inline fun <reified V: Viewing> show(
            handler:       Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler).show(init)

    protected fun show(view: Viewing) = view.display(region(io))

    protected fun show(handler: Handling, view: Viewing) =
            view.display(region(handler))

    protected inline fun <reified V: Viewing> overlay(
            noinline init: (V.() -> Unit)? = null
    ) = region(io.pushLayer).show(init)

    protected inline fun <reified V: Viewing> overlay(
            handler: Handling,
            noinline init: (V.() -> Unit)? = null
    ) = region(handler.pushLayer).show(init)

    protected fun region(handler: Handling) = ViewingRegion(handler)

    protected fun addRegion(
            region: ViewingRegion,
            init:   ((Context) -> Unit)? = null
    ) = requireContext().addRegion(region).apply {
            init?.invoke(this)
        }

    // Navigate

    protected inline fun <reified C: Controller> next(handler: Handling? = null): TargetActionPromise<C> =
            (handler ?: requireContext()).next()

    protected inline fun <reified C: Controller> next(
            handler: Handling? = null,
            noinline action: (C) -> Unit
    ) = (handler ?: requireContext()).next(action)

    protected inline fun <reified C: Controller> push(handler: Handling? = null): TargetActionPromise<C> =
            (handler ?: requireContext()).push()

    protected inline fun <reified C: Controller> push(
            handler: Handling? = null,
            noinline action: (C) -> Unit
    ) = (handler ?: requireContext()).push(action)

    protected inline fun <reified C: Controller> partial(handler: Handling? = null): TargetActionPromise<C> =
            (handler ?: requireContext()).partial()

    protected inline fun <reified C: Controller> partial(
            handler: Handling? = null,
            noinline action: (C) -> Unit
    ) = (handler ?: requireContext()).partial(action)

    protected inline fun <reified C: Controller> navigate(
            style: NavigationStyle
    ): TargetActionPromise<C> = requireContext().navigate(style)

    protected inline fun <reified C: Controller> navigate(
            style:   NavigationStyle,
            handler: Handling
    ): TargetActionPromise<C> = handler.navigate(style)

    protected inline fun <reified C: Controller> navigate(
            style:   NavigationStyle,
            noinline action: (C) -> Unit
    ) = requireContext().navigate(style, action)

    protected inline fun <reified C: Controller> navigate(
            style:   NavigationStyle,
            handler: Handling,
            noinline action: (C) -> Unit
    ) = handler.navigate(style, action)

    val noBack get() = requireContext().noBack

    fun goBack() = requireContext().goBack()

    protected fun goBack(handler: Handling) = handler.goBack()

    // Workflow

    fun finish() {
        endContext()
    }

    inline fun <reified A: Any> finish(a: A?) {
        context?.also {
            it.store(a).end()
        }
    }

    inline fun <reified A1: Any, reified A2: Any> finish(a1: A1?, a2: A2?) {
        context?.also {
            it.store(a1).store(a2).end()
        }
    }

    inline fun <reified A1: Any, reified A2: Any, reified A3: Any> finish(a1: A1?, a2: A2?, a3: A3?) {
        context?.also {
            it.store(a1).store(a2).store(a3).end()
        }
    }

    inline fun <reified A1: Any, reified A2: Any, reified A3: Any, reified A4: Any> finish(a1: A1?, a2: A2?, a3: A3?, a4: A4?) {
        context?.also {
            it.store(a1).store(a2).store(a3).store(a4).end()
        }
    }

    inline fun <reified A1: Any, reified A2: Any, reified A3: Any, reified A4: Any, reified A5: Any> finish(a1: A1?, a2: A2?, a3: A3?, a4: A4?, a5: A5?) {
        context?.also {
            it.store(a1).store(a2).store(a3).store(a4).store(a5).end()
        }
    }

    protected fun delay(delayMs: Long) =
            Promise.delay(delayMs).also { context?.track(it) }

    val disposing = Event<Controller>()
    val disposed  = Event<Controller>()

    protected open fun cleanUp() {}

    final override fun close() {
        if (_closed.compareAndSet(false, true)) {
            disposing(this)
            disposing.clear()
            cleanUp()
            contextChanging.clear()
            contextChanged.clear()
            context = null
            _io     = null
            disposed(this)
            disposed.clear()
        }
    }
}