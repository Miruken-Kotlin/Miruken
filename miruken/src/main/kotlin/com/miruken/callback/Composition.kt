package com.miruken.callback

import kotlin.reflect.KClass

class Composition(callback: Any) : Trampoline(callback),
        ICallback, IResolveCallback,
        IFilterCallback, IBatchCallback {

    override val resultType: KClass<*>?
        get() = (callback as? ICallback)?.resultType

    override var result: Any?
        get() = (callback as? ICallback)?.result
        set(value) {
            (callback as? ICallback)?.result = value
        }

    override val allowFiltering: Boolean
        get() = (callback as? IFilterCallback)?.allowFiltering != false

    override val allowBatching: Boolean
        get() = (callback as? IBatchCallback)?.allowBatching != false

    override fun getResolveCallback(): Any {
        val resolve = (callback as? IResolveCallback)?.getResolveCallback()
        if (resolve === callback) return this
        val callback = resolve ?: Resolving.getDefaultResolvingCallback(callback)
        return Composition(callback)
    }
}