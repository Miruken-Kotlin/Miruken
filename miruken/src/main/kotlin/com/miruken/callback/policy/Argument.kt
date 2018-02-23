package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.callback.ArgumentResolver
import com.miruken.runtime.getFirstTaggedAnnotation
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability

class Argument(val parameter: KParameter) {

    val key:         Any?
    val logicalType: KType
    val flags:       Flags<TypeFlags>
    val resolver:    ArgumentResolving

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations
    inline val index         get() = parameter.index

    init {
        val typeFlags = TypeFlags.parse(parameterType)
        logicalType   = typeFlags.second
        flags         = typeFlags.first

        key = if (flags has TypeFlags.PRIMITIVE)
            parameter.name else logicalType

        resolver = parameter
                .getFirstTaggedAnnotation<UseArgumentResolver<*>>()
                ?.argumentResolverClass?.objectInstance
                ?: DefaultResolver
    }

    fun satisfies(type: KType): Boolean {
        return parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))
    }

    companion object {
        object DefaultResolver : ArgumentResolver()
    }
}