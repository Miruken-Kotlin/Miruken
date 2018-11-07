package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.policy.rules.ExtractArgument
import com.miruken.callback.policy.rules.TargetArgument
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf

@CalbackPolicyDsl
class ContravariantPolicyBuilder<C: Any, out S: Any>(
        callbackType: TypeReference,
        targetType:   TypeReference,
        private val targetFunctor: (C) -> S
) : CallbackPolicyBuilder(callbackType) {

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: C.() -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): ContravariantPolicy {
        return ContravariantPolicy(rules, filters) {
            @Suppress("UNCHECKED_CAST")
            if (isCompatibleWith(callbackType, it))
                targetFunctor(it as C) else null
        }
    }
}

@CalbackPolicyDsl
class ContravariantTargetBuilder {
    inline fun <reified C: Any, reified S: Any> target(
            noinline targetFunctor: (C) -> S
    ) = ContravariantWithTargetBuilder(
            targetFunctor, typeOf<C>(), typeOf<S>())
}

@CalbackPolicyDsl
class ContravariantWithTargetBuilder<C: Any, out S: Any>(
        private val targetFunctor: (C) -> S,
        private val callbackType:  TypeReference,
        private val targetType:    TypeReference
) {
    infix fun rules(
            define: ContravariantPolicyBuilder<C,S>.() -> Unit
    ): ContravariantPolicy {
        val builder = ContravariantPolicyBuilder(
                callbackType, targetType, targetFunctor)
        define(builder)
        return builder.build()
    }
}