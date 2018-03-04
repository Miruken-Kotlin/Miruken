package com.miruken.map

import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf
import kotlin.reflect.KType

interface Mapping {
    fun map(
            source:     Any,
            targetType: KType,
            target:     Any? = null,
            format:     Any? = null
    ): Any?

    fun mapAsync(
            source:     Any,
            targetType: KType,
            target:     Any? = null,
            format:     Any? = null
    ): Promise<Any?>
}

inline fun <reified T> Mapping.map(
        source:     Any,
        format:     Any? = null
): T? = map(source, typeOf<T>(), null, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mapping.mapAsync(
        source:     Any,
        format:     Any? = null
): Promise<T?> =
        mapAsync(source, typeOf<T>(), null, format) as Promise<T?>

inline fun <reified T> Mapping.mapInto(
        source:     Any,
        target:     T,
        format:     Any? = null
): T? = map(source, typeOf<T>(), target, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mapping.mapIntoAsync(
        source:     Any,
        target:     T,
        format:     Any? = null
): Promise<T?> = mapAsync(source, typeOf<T>(), target, format) as Promise<T?>
