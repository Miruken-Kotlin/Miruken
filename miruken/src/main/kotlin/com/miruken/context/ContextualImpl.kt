package com.miruken.context

import com.miruken.event.Event

open class ContextualImpl : Contextual {
    override val contextChanging = Event<ContextChangingEvent>()
    override val contextChanged  = Event<ContextChangedEvent>()

    override var context: Context? = null
        set(value) {
            if (field == value) return
            contextChanging {
                ContextChangingEvent(this, field, value)
            }
            field?.removeHandlers(this)
            val oldContext = field
            field = value
            field?.insertHandlers(0, this)
            contextChanged {
                ContextChangedEvent(this, oldContext, field)
            }
        }
}