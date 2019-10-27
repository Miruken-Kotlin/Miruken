package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf

open class Composition(
        callback:     Any?   = null,
        callbackType: TypeReference? = null
) : Trampoline(callback, callbackType),
        InferringCallback, FilteringCallback, BatchingCallback {

    override fun getCallbackKey(): Any? =
            (callback as? Callback)?.getCallbackKey()

    override val canFilter: Boolean
        get() = (callback as? FilteringCallback)?.canFilter != false

    override val canBatch: Boolean
        get() = (callback as? BatchingCallback)?.canBatch != false

    override fun inferCallback(): Any {
        val resolve = (callback as? InferringCallback)?.inferCallback()
        if (resolve === callback || callback === null) return this
        return Composition(resolve ?: Inference.get(callback, callbackType),
                callbackType)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> get(callback: Any) =
                get(callback, typeOf<T>()) as? T

        fun get(callback: Any, key: Any) =
                (callback as? Composition)?.let { c ->
                    c.callback?.takeIf { isCompatibleWith(
                            key, c.callbackType ?: it)
                    }
            }
    }
}