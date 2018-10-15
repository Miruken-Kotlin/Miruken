package com.miruken.context

import com.miruken.callback.*
import com.miruken.event.Event
import com.miruken.graph.Traversing
import com.miruken.graph.TraversingAxis
import kotlin.reflect.KType

enum class ContextState {
    ACTIVE,
    ENDING,
    ENDED
}

data class ContextEvent(val context: Context)

open class Context() :
        CompositeHandler(), HandlingAxis,
        Traversing, AutoCloseable {
    private val _children = mutableListOf<Context>()

    private constructor(parent: Context) : this() {
        this.parent = parent
    }

    var state = ContextState.ACTIVE
        private set
    final override var parent: Context? = null
        private set

    final override val children: List<Context>
        get() = _children.toList()

    val hasChildren: Boolean
        get() = children.isNotEmpty()

    val contextEnding       = Event<ContextEvent>()
    val contextEnded        = Event<ContextEvent>()
    val childContextEnding  = Event<ContextEvent>()
    val childContextEnded   = Event<ContextEvent>()

    val root: Context get() {
        var root: Context = this
        while (root.parent != null)
            root = root.parent as Context
        return root
    }

    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        return super.handleCallback(
                callback, callbackType, greedy, composer)
                .otherwise(greedy) {
                    parent?.handle(callback, callbackType, greedy, composer)
                        ?: HandleResult.NOT_HANDLED
                }
    }

    override fun handle(
            axis:         TraversingAxis,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling?
    ): HandleResult {
        val scope = composer ?: CompositionScope(this)

        if (axis == TraversingAxis.SELF)
            return super.handleCallback(
                    callback, callbackType, greedy, scope)

        var result = HandleResult.NOT_HANDLED
        traverse(axis) {
            result = result or when {
                it === this ->
                    super.handleCallback(
                            callback, callbackType, greedy, scope)
                it is Context ->
                    it.handle(TraversingAxis.SELF, callback,
                            callbackType, greedy, scope)
                else -> result
            }
            result.stop || (result.handled && !greedy)
        }
        return result
    }

    fun store(data: Any) {
        addHandlers(data)
    }

    fun createChild(): Context {
        requireActive()
        val child = Context(this)
        child.contextEnding += { (ctx) ->
            childContextEnding { ContextEvent(ctx) }
        }
        child.contextEnded += { (ctx) ->
            _children.remove(ctx)
            childContextEnded { ContextEvent(ctx) }
        }
        _children.add(child)
        return child
    }

    fun unwindToRoot(): Context {
        var current: Context? = this
        while (current != null) {
            val parent = current.parent
            if (parent == null)  {
                current.unwind()
                return current
            }
            current = parent
        }
        return this
    }

    fun unwind(): Context {
        for (child in children) child.end()
        return this
    }

    fun end() {
        if (state != ContextState.ACTIVE) return
        state = ContextState.ENDING
        contextEnding { ContextEvent(this) }
        try {
            unwind()
        } finally {
            state = ContextState.ENDED
            contextEnded { ContextEvent(this) }
            contextEnding.clear()
            contextEnded.clear()
            childContextEnding.clear()
            childContextEnded.clear()
        }
    }

    override fun close() = end()

    private fun requireActive() {
        if (state !== ContextState.ACTIVE)
            error("The context has already ended")
    }
}