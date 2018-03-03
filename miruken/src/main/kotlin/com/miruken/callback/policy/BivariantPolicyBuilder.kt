package com.miruken.callback.policy

import com.miruken.runtime.typeOf
import kotlin.reflect.KType

class BivariantPolicyBuilder<C: Any, out S: Any>(
        policy:        BivariantPolicy,
        keyFunctor:    (C) -> Any,
        targetFunctor: (C) -> S,
        callbackType:  KType,
        targetType:    KType
) : CallbackPolicyBuilder(policy, callbackType) {

    init {
        @Suppress("UNCHECKED_CAST")
        policy.output = CovariantPolicy().also {
            it.keyFunctor = keyFunctor as (Any) -> Any?
        }
        @Suppress("UNCHECKED_CAST")
        policy.input  = ContravariantPolicy().also {
            it.targetFunctor = targetFunctor as (Any) -> Any?
        }
    }

    val key  = ReturnsKey
    val unit = ReturnsUnit

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)
}

class BivariantKeyBuilder(val policy: BivariantPolicy) {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = BivariantWithKeyBuilder(policy, keyFunctor,  typeOf<C>())
}

@Suppress("MemberVisibilityCanBePrivate")
class BivariantWithKeyBuilder<in C: Any>(
        val policy:       BivariantPolicy,
        val keyFunctor:   (C) -> Any,
        val callbackType: KType
) {
    inline fun <reified D: C, reified S: Any> target(
            noinline targetFunctor: (D) -> S
    ) = BivariantWithKeyTargetBuilder(policy, keyFunctor,
            targetFunctor, typeOf<D>(), typeOf<S>())
}

@Suppress("MemberVisibilityCanBePrivate")
class BivariantWithKeyTargetBuilder<C: Any, out S: Any>(
        val policy:        BivariantPolicy,
        val keyFunctor:    (C) -> Any,
        val targetFunctor: (C) -> S,
        val callbackType:  KType,
        val targetType:    KType
) {
    inline infix fun rules(
            build: BivariantPolicyBuilder<C,S>.() -> Unit
    ): CallbackPolicyBuilder.Completed {
        val builder = BivariantPolicyBuilder(policy, keyFunctor,
                targetFunctor, callbackType, targetType)
        builder.build()
        return CallbackPolicyBuilder.Completed
    }
}