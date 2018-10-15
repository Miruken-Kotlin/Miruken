package com.miruken.context

import com.miruken.event.Event

open class ContextualImpl : Contextual {
    private var _context: Context? = null

    override val contextChanging = Event<ContextChangingEvent>()
    override val contextChanged  = Event<ContextChangedEvent>()

    override var context: Context?
        get() = _context
        set(value) {
            if (_context == value) return
            contextChanging {
                ContextChangingEvent(this, _context, value)
            }
            _context?.removeHandlers(this)
            val oldContext = _context
            _context = value
            _context?.insertHandlers(0, this)
            contextChanged {
                ContextChangedEvent(this, oldContext, _context)
            }
        }
}