package com.miruken.map

import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.runtime.typeOf
import kotlin.reflect.KType

interface Mapping {
    fun map(
            source:     Any,
            targetType: KType,
            sourceType: KType? = null,
            target:     Any?   = null,
            format:     Any?   = null
    ): Any?

    fun mapAsync(
            source:     Any,
            targetType: KType,
            sourceType: KType? = null,
            target:     Any?   = null,
            format:     Any?   = null
    ): Promise<Any?>

    companion object {
        val PROTOCOL = typeOf<Mapping>()
        operator fun invoke(adapter: ProtocolAdapter) =
                Protocol.proxy(adapter, PROTOCOL) as Mapping
    }
}

inline fun <reified T> Mapping.map(
        source:     Any,
        format:     Any?   = null,
        sourceType: KType? = null
) = map(source, typeOf<T>(), sourceType, null, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Mapping.mapAsync(
        source:     Any,
        format:     Any?   = null,
        sourceType: KType? = null
) = mapAsync(source, typeOf<T>(), sourceType, null, format) as Promise<T?>

inline fun <reified T: Any> Mapping.mapInto(
        source:     Any,
        target:     T,
        format:     Any?   = null,
        sourceType: KType? = null
) = map(source, typeOf<T>(), sourceType, target, format) as? T

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Mapping.mapIntoAsync(
        source:     Any,
        target:     T,
        format:     Any?   = null,
        sourceType: KType? = null
) = mapAsync(source, typeOf<T>(), sourceType, target, format) as Promise<T?>
