package com.miruken.context

import com.miruken.callback.Handler
import com.miruken.event.Event
import java.util.concurrent.atomic.AtomicBoolean

open class ContextualHandler :
        Handler(), Contextual, AutoCloseable {
    private var _context: Context? = null
    private val _closed = AtomicBoolean()

    override val contextChanging = Event<ContextChangingEvent>()
    override val contextChanged  = Event<ContextChangedEvent>()

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

    override fun close() {
        if (_closed.compareAndSet(false, true)) {
            contextChanging.clear()
            contextChanged.clear()
            context = null
            cleanUp()
        }
    }

    open fun failedInitialize(t: Throwable?) = close()

    open fun cleanUp() {}
}