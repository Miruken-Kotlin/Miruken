package com.miruken.map

import com.miruken.callback.Handling
import com.miruken.callback.NotHandledException
import com.miruken.callback.handle
import com.miruken.concurrent.Promise
import com.miruken.typeOf
import kotlin.reflect.KType

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