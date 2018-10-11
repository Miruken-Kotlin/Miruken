package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.ConstraintBuilder
import com.miruken.callback.policy.bindings.ConstraintProvider
import com.miruken.protocol.Protocol
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

class Argument(val parameter: KParameter) : KAnnotatedElement {
    val typeInfo    = TypeFlags.parse(parameter.type)
    val useResolver = KeyResolver.getResolverClass(parameter)
                   ?: impliedProtocolResolver()

    inline   val parameterType get() = parameter.type
    inline   val index         get() = parameter.index
    inline   val isOpen        get() = typeInfo.flags has TypeFlags.OPEN
    override val annotations   get() = parameter.annotations

    var constraints: (ConstraintBuilder.() -> Unit)? = null
        private set

    init {
        val constraints = getFilterProviders()
                .asSequence()
                .filterIsInstance<ConstraintProvider>()
                .map { it.constraint }
                .toList()

        if (constraints.isNotEmpty()) {
            this.constraints = {
                constraints.forEach { require(it) }
            }
        }
    }

    fun satisfies(type: KType) =
        parameterType.classifier != Nothing::class &&
            parameterType.isSubtypeOf(type.withNullability(true))

    fun getInquiry(
            parent:       Inquiry? = null,
            typeBindings: Map<KTypeParameter, KType>? = null
    ) = KeyResolver.getKey(parameter, typeInfo,
                parameter.name, typeBindings)?.let {
        val flags = typeInfo.flags
        Inquiry(it, flags has TypeFlags.COLLECTION ||
                flags has TypeFlags.ARRAY, parent).apply {
            wantsAsync = flags has TypeFlags.PROMISE
            constraints?.invoke(ConstraintBuilder(this))
        }
    }

    private fun impliedProtocolResolver(): KClass<out KeyResolving>? {
        if (typeInfo.flags has TypeFlags.INTERFACE) {
            val parameterClass = parameterType.jvmErasure
            if (parameterClass.findAnnotation<Protocol>() != null ||
                parameterClass.findAnnotation<Resolving>() != null)
                return ProxyKeyResolver::class
        }
        return null
    }
}