package com.miruken.map

import com.miruken.callback.AsyncCallback
import com.miruken.callback.Callback
import com.miruken.callback.DispatchingCallback
import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class MapFrom(
        val source:     Any,
        val targetType: KType,
        val sourceType: KType? = null,
        val target:     Any?   = null,
        val format:     Any?   = null
) : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null

    override var wantsAsync: Boolean = false

    override var isAsync: Boolean = false
        private set

    override val policy get() = MapsPolicy

    override fun getCallbackKey() =
            sourceType?.let { targetType to it }

    override val resultType: KType? = targetType
            .takeIf { !wantsAsync && !isAsync }
            ?: Promise::class.createType(listOf(
                    KTypeProjection.invariant(targetType)))

    override var result: Any?
        get() {
            if (wantsAsync && !isAsync)
                _result = Promise.resolve(_result)
            return _result
        }
        set(value) {
            _result = value
            isAsync = _result is Promise<*>
        }

    @Suppress("UNUSED_PARAMETER")
    fun mapped(mapping: Any, strict: Boolean): Boolean {
        result = mapping
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = MapsPolicy.dispatch(handler, this, callbackType, greedy,
            composer, ::mapped).otherwise(_result != null)
}
