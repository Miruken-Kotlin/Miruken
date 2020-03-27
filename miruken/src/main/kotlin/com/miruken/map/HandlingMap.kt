package com.miruken.map

import com.miruken.TypeReference
import com.miruken.callback.Handling
import com.miruken.callback.NotHandledException
import com.miruken.callback.handle
import com.miruken.callback.with
import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.typeOf
import kotlin.coroutines.coroutineContext

inline fun <reified T> Handling.map(
        source:     Any,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = map(source, typeOf<T>(), sourceType, null, format) as? T

fun Handling.map(
        source:     Any,
        targetType: TypeReference,
        sourceType: TypeReference? = null,
        target:     Any?           = null,
        format:     Any?           = null
): Any? {
    val mapping = Mapping(source, targetType,
            sourceType, target, format)
    return handle(mapping) success { return mapping.result }
            ?: throw NotHandledException(mapping)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.mapAsync(
        source:     Any,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = mapAsync(source, typeOf<T>(), sourceType, null, format) as Promise<T?>

fun Handling.mapAsync(
        source:     Any,
        targetType: TypeReference,
        sourceType: TypeReference? = null,
        target:     Any?           = null,
        format:     Any?           = null
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

suspend fun Handling.mapCo(
        source:     Any,
        targetType: TypeReference,
        sourceType: TypeReference? = null,
        target:     Any?           = null,
        format:     Any?           = null
) = with(coroutineContext)
        .mapAsync(source, targetType, sourceType, target, format)
        .await()

suspend inline fun <reified T: Any> Handling.mapCo(
        source:     Any,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = with(coroutineContext)
        .mapAsync<T>(source, format, sourceType)
        .await()

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.mapIntoAsync(
        source:     Any,
        target:     T,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = mapAsync(source, typeOf<T>(), sourceType, target, format) as Promise<T?>

inline fun <reified T: Any> Handling.mapInto(
        source:     Any,
        target:     T,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = map(source, typeOf<T>(), sourceType, target, format) as? T

suspend inline fun <reified T: Any> Handling.mapIntoCo(
        source:     Any,
        target:     T,
        format:     Any?           = null,
        sourceType: TypeReference? = null
) = with(coroutineContext)
        .mapAsync(source, typeOf<T>(), sourceType, target, format)
        .await()
