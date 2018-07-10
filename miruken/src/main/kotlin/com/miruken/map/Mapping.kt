package com.miruken.map

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class Mapping(
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
            if (isAsync) {
                if (!wantsAsync) {
                    _result = (_result as? Promise<*>)?.get()
                }
            } else if (wantsAsync) {
                _result = _result?.let { Promise.resolve(it) }
                        ?: Promise.EMPTY
            }
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

fun Handling.map(
        source:     Any,
        targetType: KType,
        sourceType: KType? = null,
        target:     Any?   = null,
        format:     Any?   = null
): Any? {
    val mapping = Mapping(source, targetType,
            sourceType, target, format)
    return handle(mapping) success { return mapping.result }
            ?: throw NotHandledException(mapping)
}
