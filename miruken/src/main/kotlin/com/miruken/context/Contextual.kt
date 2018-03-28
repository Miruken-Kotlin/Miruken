package com.miruken.context

import com.miruken.event.Event

data class ContextChangingEvent<T: Context>(
        val contextual: Contextual<T>,
        val oldContext: T?,
        var newContext: T?)

data class ContextChangedEvent<T: Context>(
        val contextual: Contextual<T>,
        val oldContext: T?,
        val newContext: T?)

interface Contextual<T: Context> {
    var context:         T?
    val contextChanging: Event<ContextChangingEvent<T>>
    val contextChanged:  Event<ContextChangedEvent<T>>
}

fun <T: Context> Contextual<T>.getContext(required: Boolean = false) =
        context ?: if (required) {
            error("Required context is not available")
        } else {
            null
        }