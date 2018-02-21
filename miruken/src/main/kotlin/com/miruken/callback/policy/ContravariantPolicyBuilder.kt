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

    inline fun <reified E: Any> extract (noinline block: (C) -> E) =
            ExtractArgument(getKType<E>(), block)
}

class ContravariantTargetBuilder(val policy: ContravariantPolicy) {
    inline fun <reified C: Any, reified S: Any> target(
            noinline targetFunctor: (C) -> S,
            build: ContravariantPolicyBuilder<C, S>.() -> Unit
    ) {
        val builder = ContravariantPolicyBuilder(
                policy, targetFunctor, getKType<C>(), getKType<S>())
        builder.build()
    }
}