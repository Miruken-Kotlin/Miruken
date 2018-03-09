package com.miruken.callback

import com.miruken.typeOf
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

open class Composition(callback: Any? = null): Trampoline(callback),
        Callback, ResolvingCallback,
        FilteringCallback, BatchingCallback {

    override val resultType: KType?
        get() = (callback as? Callback)?.resultType

    override var result: Any?
        get() = (callback as? Callback)?.result
        set(value) {
            (callback as? Callback)?.result = value
        }

    override fun getCallbackKey(): Any? =
            (callback as? Callback)?.getCallbackKey()

    override val canFilter: Boolean
        get() = (callback as? FilteringCallback)?.canFilter != false

    override val canBatch: Boolean
        get() = (callback as? BatchingCallback)?.canBatch != false

    override fun getResolveCallback(): Any {
        val resolve = (callback as? ResolvingCallback)?.getResolveCallback()
        if (resolve === callback || callback === null) return this
        return Composition(resolve
                ?: Resolution.getDefaultResolvingCallback(callback))
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> get(callback: Any): T? =
                get(callback, typeOf<T>()) as? T

        fun get(callback: Any, key: Any): Any? {
            return (callback as? Composition)?.let { c ->
                c.callback?.takeIf {
                    isCompatibleWith(key, c.resultType ?: it)
                }
            }
        }
    }
}