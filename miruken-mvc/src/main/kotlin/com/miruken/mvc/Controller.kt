package com.miruken.mvc

import com.miruken.callback.Handling
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

    val async get() = requireContext().async

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
            init:   ((Context) -> Unit)? = null
    ) = requireContext().addRegion(region).apply {
            init?.invoke(this)
        }

    // Navigate

    protected inline fun <reified C: Controller> next() =
            requireContext().next<C>()

    protected inline fun <reified C: Controller> next(handler: Handling ) =
            handler.next<C>()

    protected inline fun <reified C: Controller> next(noinline action: C.() -> Unit) =
            requireContext().next(action)

    protected inline fun <reified C: Controller> next(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.next(action)

    protected inline fun <reified C: Controller> push() =
            requireContext().push<C>()

    protected inline fun <reified C: Controller> push(handler: Handling) =
            handler.push<C>()

    protected inline fun <reified C: Controller> push(noinline action: C.() -> Unit) =
            requireContext().push(action)

    protected inline fun <reified C: Controller> push(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.push(action)

    protected inline fun <reified C: Controller> partial() =
            requireContext().partial<C>()

    protected inline fun <reified C: Controller> partial(handler: Handling) =
            handler.partial<C>()

    protected inline fun <reified C: Controller> partial(noinline action: C.() -> Unit) =
            requireContext().partial(action)

    protected inline fun <reified C: Controller> partial(
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.partial(action)

    protected inline fun <reified C: Controller> navigate(
            style: NavigationStyle
    ) = requireContext().navigate<C>(style)

    protected inline fun <reified C: Controller> navigate(
            style:   NavigationStyle,
            handler: Handling
    ) = handler.navigate<C>(style)

    protected inline fun <reified C: Controller> navigate(
            style:   NavigationStyle,
            noinline action: C.() -> Unit
    ) = requireContext().navigate(style, action)

    protected inline fun <reified C: Controller> navigate(
            style:           NavigationStyle,
            handler:         Handling,
            noinline action: C.() -> Unit
    ) = handler.navigate(style, action)

    val noBack get() = requireContext().noBack

    fun goBack() = requireContext().goBack()

    protected fun goBack(handler: Handling) = handler.goBack()

    val disposing = Event<Controller>()
    val disposed  = Event<Controller>()

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

    open fun cleanUp() {}
}