package com.miruken.context

import com.miruken.callback.CompositeHandler
import com.miruken.callback.CompositionScope
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.event.Event
import com.miruken.graph.TraversingAxis
import kotlin.reflect.KType

open class ContextImpl() : CompositeHandler(), Context {
    private val _children = mutableListOf<ContextImpl>()

    private constructor(parent: ContextImpl) : this() {
        this.parent = parent
    }

    final override var state:  ContextState = ContextState.ACTIVE
        private set
    final override var parent: ContextImpl? = null
        private set

    final override val children: List<ContextImpl>
        get() = _children.toList()

    final override val hasChildren: Boolean
        get()  = children.isNotEmpty()

    final override val contextEnding       = Event<ContextEvent>()
    final override val contextEnded        = Event<ContextEvent>()
    final override val childContextEnding  = Event<ContextEvent>()
    final override val childContextEnded   = Event<ContextEvent>()

    final override val root: ContextImpl get() {
        var root = this
        while (root.parent != null)
            root = root.parent as ContextImpl
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

    override fun store(data: Any) {
        addHandlers(data)
    }

    override fun createChild(): Context {
        requireActive()
        val child = ContextImpl(this)
        child.contextEnding += { (ctx) ->
            childContextEnding(ContextEvent(ctx))
        }
        child.contextEnded += { (ctx) ->
            _children.remove(ctx)
            childContextEnded(ContextEvent(ctx))
        }
        _children.add(child)
        return child
    }

    final override fun unwindToRoot(): ContextImpl {
        var current: ContextImpl? = this
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

    final override fun unwind(): ContextImpl {
        for (child in children) child.end()
        return this
    }

    final override fun end() {
        if (state != ContextState.ACTIVE) return
        state = ContextState.ENDING
        val event = ContextEvent(this)
        contextEnding(event)
        try {
            unwind()
        } finally {
            state = ContextState.ENDED
            contextEnded(event)
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