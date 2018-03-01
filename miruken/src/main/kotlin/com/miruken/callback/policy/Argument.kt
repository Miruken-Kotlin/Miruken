package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.callback.Key
import com.miruken.callback.StringKey
import com.miruken.callback.UseArgumentResolver
import com.miruken.runtime.getFirstTaggedAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability

class Argument(val parameter: KParameter) {

    val key:         Any?
    val logicalType: KType
    val flags:       Flags<TypeFlags>
    val useResolver: KClass<out ArgumentResolving>?

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations
    inline val index         get() = parameter.index

    init {
        val typeFlags = TypeFlags.parse(parameterType)
        logicalType   = typeFlags.second
        flags         = typeFlags.first

        key = parameter.annotations
                .firstOrNull { it is Key }
                ?.let {
                    val key = it as Key
                    StringKey(key.key, key.caseSensitive)
                } ?: parameter.name?.takeIf {
                    flags has TypeFlags.PRIMITIVE
                } ?: logicalType

        useResolver = parameter
                .getFirstTaggedAnnotation<UseArgumentResolver<*>>()
                ?.argumentResolverClass
    }

    fun satisfies(type: KType): Boolean {
        return parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))
    }
}