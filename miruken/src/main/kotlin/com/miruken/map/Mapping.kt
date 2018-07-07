package com.miruken.map

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.typeOf
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

fun Handling.mapAsync(
        source:     Any,
        targetType: KType,
        sourceType: KType? = null,
        target:     Any?   = null,
        format:     Any?   = null
): Promise<Any?> {
    val mapping = Mapping(source, targetType,
            sourceType, target, format).apply {
        wantsAsync = true
    }
    return handle(mapping) success {
        @Suppress("UNCHECKED_CAST")
        return mapping.result as Promise<Any?>
    } ?: Promise.reject(NotHandledException(mapping))
}

inline fun <reified T> Handling.map(
        source:     Any,
        format:     Any?   = null,
        sourceType: KType? = null
) = map(source, typeOf<T>(), sourceType, null, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.mapAsync(
        source:     Any,
        format:     Any?   = null,
        sourceType: KType? = null
) = mapAsync(source, typeOf<T>(), sourceType, null, format) as Promise<T?>

inline fun <reified T: Any> Handling.mapInto(
        source:     Any,
        target:     T,
        format:     Any?   = null,
        sourceType: KType? = null
) = map(source, typeOf<T>(), sourceType, target, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.mapIntoAsync(
        source:     Any,
        target:     T,
        format:     Any?   = null,
        sourceType: KType? = null
) = mapAsync(source, typeOf<T>(), sourceType, target, format) as Promise<T?>