package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.KeyResolver
import com.miruken.callback.KeyResolving
import com.miruken.callback.ProxyKeyResolver
import com.miruken.callback.Resolving
import com.miruken.protocol.Protocol
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

class Argument(val parameter: KParameter) {
    val typeInfo    = TypeFlags.parse(parameter.type)
    val key         = KeyResolver.getKey(parameter, typeInfo, parameter.name)
    val useResolver = KeyResolver.getResolverClass(parameter)
                   ?: impliedProtocolResolver()

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations
    inline val index         get() = parameter.index

    fun satisfies(type: KType) =
        parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))

    private fun impliedProtocolResolver(): KClass<out KeyResolving>? {
        if (typeInfo.flags.has(TypeFlags.INTERFACE)) {
            val parameterClass = parameterType.jvmErasure
            if (parameterClass.findAnnotation<Protocol>() != null ||
                parameterClass.findAnnotation<Resolving>() != null)
                return ProxyKeyResolver::class
        }
        return null
    }
}