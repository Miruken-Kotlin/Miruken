package com.miruken.mvc.policy

import com.miruken.event.Event
import java.util.concurrent.atomic.AtomicInteger

open class DefaultPolicy : Policy {
    private val _dependencies = mutableListOf<Policy>()
    private val _releasing    = AtomicInteger()
    private val _onRelease    = Event<DefaultPolicy>()

    override fun track(): Policy {
        isTracked = true
        return this
    }

    override fun retain(): Policy {
        isTracked = false
        return this
    }

    final override var isTracked = false
        private set

    final override var parent: Policy? = null

    final override val dependencies
        get() = _dependencies.toList()

    override fun isDependency(dependency: Policy) =
            _dependencies.contains(dependency) ||
                    _dependencies.any { it.isDependency(dependency) }

    override fun addDependency(dependency: Policy): Policy {
        if (dependency.parent == this || dependency == this)
            return this

        if (dependency.isDependency(this))
            throw IllegalStateException("Cyclic dependency detected")

        dependency.parent?.removeDependency(dependency)

        if (!_dependencies.contains(dependency))
            _dependencies.add(dependency)
        dependency.parent = this
        return this
    }

    override fun removeDependency(dependency: Policy): Policy {
        if (dependency.parent != this) return this
        _dependencies.remove(dependency)
        dependency.parent = null
        return this
    }

    override fun onRelease(onRelease: () -> Unit): () -> Unit {
        return _onRelease.register { _ -> onRelease() }
    }

    override fun release() {
        if (!_releasing.compareAndSet(0, 1)) return

        parent?.removeDependency(this)

        if (isTracked) {
            for (dependency in dependencies)
                dependency.release()

            _onRelease.invoke(this)

            reset()
        } else {
            _releasing.set(0)
        }
    }

    protected fun autoRelease(doRelease: () -> Unit): DefaultPolicy {
        var released = false
        onRelease {
            if (!released) {
                released = true
                doRelease()
            }
        }
        return this
    }

    private fun reset() {
        parent    = null
        isTracked = false
        _dependencies.clear()
        _onRelease.clear()
        _releasing.set(0)
    }
}