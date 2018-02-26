package com.miruken.context

import com.miruken.event.Event

open class ContextualImpl<T : Context> : Contextual<T> {
    private var _context: T? = null

    override val contextChanging = Event<ContextChangingEvent<T>>()
    override val contextChanged  = Event<ContextChangedEvent<T>>()

    override var context: T?
        get() = _context
        set(value) {
            if (_context == value) return
            val changingEvent = ContextChangingEvent(this, _context, value)
            contextChanging(changingEvent)
            _context?.removeHandlers(this)
            val oldContext = _context
            _context = changingEvent.newContext
            _context?.insertHandlers(0, this)
            contextChanged(ContextChangedEvent(this, oldContext, _context))
        }
}