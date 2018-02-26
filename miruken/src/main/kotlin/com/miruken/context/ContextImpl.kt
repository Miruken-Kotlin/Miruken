package com.miruken.context

import com.miruken.callback.CompositeHandler
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.event.Event
import com.miruken.graph.TraversingAxis

/**
open class ContextImpl : CompositeHandler(), Context {

    private val _contextEnding       = lazy { Event<ContextEvent>() }
    private val _contextEnded        = lazy { Event<ContextEvent>() }
    private val _childContextEnding  = lazy { Event<ContextEvent>() }
    private val _childContextEnded   = lazy { Event<ContextEvent>() }
    private val _children            = mutableListOf<ContextImpl>()

    private constructor(parent: ContextImpl) {
        this.parent = parent
    }

    final override val state: ContextState  = ContextState.ACTIVE
    final override var parent: ContextImpl? = null
        private set
    final override val children: List<ContextImpl> = _children

    final override val contextEnding      get() = _contextEnding.value
    final override val contextEnded       get() = _contextEnded.value
    final override val childContextEnding get() = _childContextEnding.value
    final override val childContextEnded  get() = _childContextEnded.value

    final override val hisChildren: Boolean get() = children.isNotEmpty()

    final override val root: ContextImpl get() {
        var root = this
        while (root.parent != null)
            root = root.parent as ContextImpl
        return root
    }

    override fun createChild(): Context {
        assertActive()
        val child = internalCreateChild()
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

    protected open fun internalCreateChild(): ContextImpl = ContextImpl()

    private fun assertActive() {
        if (state !== ContextState.ACTIVE)
            throw IllegalStateException("The context has already ended")
    }
}
        */