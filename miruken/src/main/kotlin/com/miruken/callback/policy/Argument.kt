package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.KeyResolver
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability

class Argument(val parameter: KParameter) {
    val typeInfo    = TypeFlags.parse(parameter.type)
    val key         = KeyResolver.getKey(parameter, typeInfo, parameter.name)
    val useResolver = KeyResolver.getResolverClass(parameter)

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations
    inline val index         get() = parameter.index

    fun satisfies(type: KType) =
        parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))
}