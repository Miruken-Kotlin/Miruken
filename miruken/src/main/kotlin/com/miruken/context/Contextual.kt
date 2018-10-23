package com.miruken.context

import com.miruken.event.Event

data class ContextChangingEvent(
        val contextual: Contextual,
        val oldContext: Context?,
        var newContext: Context?)

data class ContextChangedEvent(
        val contextual: Contextual,
        val oldContext: Context?,
        val newContext: Context?)

interface Contextual {
    var context:         Context?
    val contextChanging: Event<ContextChangingEvent>
    val contextChanged:  Event<ContextChangedEvent>
}

fun Contextual.getContext(required: Boolean = false) =
        context ?: if (required) {
            error("Required context is not available")
        } else {
            null
        }

fun Contextual.requireContext() = getContext(true)!!