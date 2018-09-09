package com.miruken.callback.policy

import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf
import kotlin.reflect.KType

class BivariantPolicyBuilder<C: Any, out S: Any>(
        callbackType:  KType,
        targetType:    KType,
        targetFunctor: C.() -> S,
        private val output: CovariantPolicy,
        private val input:  ContravariantPolicy
) : CallbackPolicyBuilder(callbackType) {

    val key  = ReturnsKey
    val unit = ReturnsUnit

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): BivariantPolicy {
        return BivariantPolicy(rules, filters, output, input)
    }
}

class BivariantKeyBuilder {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = BivariantWithKeyBuilder(keyFunctor, typeOf<C>())
}

class BivariantWithKeyBuilder<C: Any>(
        val keyFunctor:   (C) -> Any,
        val callbackType: KType
) {
    inline infix fun <reified S: Any> target(
            noinline targetFunctor: C.() -> S
    ) = BivariantWithKeyTargetBuilder(keyFunctor,
            targetFunctor, callbackType, typeOf<S>())
}

class BivariantWithKeyTargetBuilder<C: Any, out S: Any>(
        private val keyFunctor:    (C) -> Any,
        private val targetFunctor: C.() -> S,
        private val callbackType:  KType,
        private val targetType:    KType
) {
    infix fun rules(
            define: BivariantPolicyBuilder<C,S>.() -> Unit
    ): BivariantPolicy {
        val co = CovariantPolicy(emptyList(), emptyList()) {
            @Suppress("UNCHECKED_CAST")
            if (isCompatibleWith(callbackType, it))
                keyFunctor(it as C) else null
        }
        val contra = ContravariantPolicy(emptyList(), emptyList()) {
            @Suppress("UNCHECKED_CAST")
            if (isCompatibleWith(callbackType, it))
                targetFunctor(it as C) else null
        }
        val builder = BivariantPolicyBuilder(callbackType, targetType,
                targetFunctor, co, contra)
        define(builder)
        return builder.build()
    }
}