package com.miruken.callback

import kotlin.reflect.KClass

class Composition(callback: Any) : Trampoline(callback),
        Callback, Resolving,
        Filtering, Batching {

    override val resultType: KClass<*>?
        get() = (callback as? Callback)?.resultType

    override var result: Any?
        get() = (callback as? Callback)?.result
        set(value) {
            (callback as? Callback)?.result = value
        }

    override val allowFiltering: Boolean
        get() = (callback as? Filtering)?.allowFiltering != false

    override val allowBatching: Boolean
        get() = (callback as? Batching)?.allowBatching != false

    override fun getResolveCallback(): Any {
        val resolve = (callback as? Resolving)?.getResolveCallback()
        if (resolve === callback) return this
        val callback = resolve ?: Resolution.getDefaultResolvingCallback(callback)
        return Composition(callback)
    }
}