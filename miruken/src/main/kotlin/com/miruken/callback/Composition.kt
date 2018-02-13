package com.miruken.callback

import kotlin.reflect.KClass

class Composition(callback: Any) : Trampoline(callback),
        Callback, ResolvingCallback,
        FilteringCallback, BatchingCallback {

    override val resultType: KClass<*>?
        get() = (callback as? Callback)?.resultType

    override var result: Any?
        get() = (callback as? Callback)?.result
        set(value) {
            (callback as? Callback)?.result = value
        }

    override val allowFiltering: Boolean
        get() = (callback as? FilteringCallback)?.allowFiltering != false

    override val allowBatching: Boolean
        get() = (callback as? BatchingCallback)?.allowBatching != false

    override fun getResolveCallback(): Any {
        val resolve = (callback as? ResolvingCallback)?.getResolveCallback()
        if (resolve === callback) return this
        val callback = resolve ?: Resolution.getDefaultResolvingCallback(callback)
        return Composition(callback)
    }
}