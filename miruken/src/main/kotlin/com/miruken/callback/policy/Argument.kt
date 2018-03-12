package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.Key
import com.miruken.callback.KeyResolving
import com.miruken.callback.StringKey
import com.miruken.callback.UseKeyResolver
import com.miruken.runtime.getFirstTaggedAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability

class Argument(val parameter: KParameter) {

    val key:         Any
    val useResolver: KClass<out KeyResolving>?
    val typeInfo = TypeFlags.parse(parameter.type)

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations
    inline val index         get() = parameter.index

    init {
        key = parameter.annotations
                .firstOrNull { it is Key }
                ?.let {
                    val key = it as Key
                    StringKey(key.key, key.caseSensitive)
                } ?: parameter.name?.takeIf {
                    typeInfo.flags has TypeFlags.PRIMITIVE
                } ?: typeInfo.componentType

        useResolver = parameter
                .getFirstTaggedAnnotation<UseKeyResolver>()
                ?.keyResolverClass
    }

    fun satisfies(type: KType): Boolean {
        return parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))
    }
}