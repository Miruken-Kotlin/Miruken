package com.miruken.callback.policy

import com.miruken.runtime.getKType
import kotlin.reflect.KType

class ContravariantPolicyBuilder<C: Any, out S: Any>(
        policy:        ContravariantPolicy,
        targetFunctor: (C) -> S,
        callbackType:  KType,
        targetType:    KType
) : CallbackPolicyBuilder(policy, callbackType) {

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(getKType<E>(), block)
}

class ContravariantTargetBuilder(val policy: ContravariantPolicy
) {
    inline fun <reified C: Any, reified S: Any> target(
            noinline targetFunctor: (C) -> S
    ) = ContravariantWithTargetBuilder(policy,
            targetFunctor, getKType<C>(), getKType<S>())
}

@Suppress("MemberVisibilityCanBePrivate")
class ContravariantWithTargetBuilder<C: Any, out S: Any>(
        val policy:        ContravariantPolicy,
        val targetFunctor: (C) -> S,
        val callbackType:  KType,
        val targetType:    KType
) {
    inline infix fun rules(build: ContravariantPolicyBuilder<C, S>.() -> Unit) {
        val builder = ContravariantPolicyBuilder(policy,
                targetFunctor, callbackType, targetType)
        builder.build()
    }
}