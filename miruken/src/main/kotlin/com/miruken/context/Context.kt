package com.miruken.context

import com.miruken.callback.CompositeHandling
import com.miruken.callback.HandlingAxis
import com.miruken.event.Event
import com.miruken.graph.Traversing

data class ContextEvent(val context: Context)

interface Context :
        CompositeHandling, HandlingAxis,
        Traversing, AutoCloseable {

    val state:              ContextState
    val root:               Context
    val hasChildren:        Boolean
    val contextEnding:      Event<ContextEvent>
    val contextEnded:       Event<ContextEvent>
    val childContextEnding: Event<ContextEvent>
    val childContextEnded:  Event<ContextEvent>

    override val parent:   Context?
    override val children: List<Context>

    fun createChild(): Context

    fun store(data: Any)

    fun unwindToRoot(): Context

    fun unwind(): Context

    fun end()
}