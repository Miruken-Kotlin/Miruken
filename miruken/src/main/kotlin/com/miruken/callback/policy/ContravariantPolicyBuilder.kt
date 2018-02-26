package com.miruken.callback.policy

import com.miruken.runtime.typeOf
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class ContravariantPolicyBuilder<C: Any, out S: Any>(
        policy:        ContravariantPolicy,
        targetFunctor: (C) -> S,
        callbackType:  KType,
        targetType:    KType
) : CallbackPolicyBuilder(policy, callbackType) {

    init {
        policy.targetFunctor = {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(targetFunctor, it))
                targetFunctor(it as C) else null
        }
    }

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)
}

class ContravariantTargetBuilder(val policy: ContravariantPolicy
) {
    inline fun <reified C: Any, reified S: Any> target(
            noinline targetFunctor: (C) -> S
    ) = ContravariantWithTargetBuilder(policy,
            targetFunctor, typeOf<C>(), typeOf<S>())
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