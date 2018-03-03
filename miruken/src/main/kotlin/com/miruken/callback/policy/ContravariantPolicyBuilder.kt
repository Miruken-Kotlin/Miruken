package com.miruken.callback.policy

import com.miruken.runtime.typeOf
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class ContravariantPolicyBuilder<C: Any, out S: Any>(
        callbackType: KType,
        targetType:   KType,
        private val targetFunctor: (C) -> S
) : CallbackPolicyBuilder(callbackType) {

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): ContravariantPolicy {
        return ContravariantPolicy(rules, filters, {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                targetFunctor(it as C) else null
        })
    }
}

class ContravariantTargetBuilder {
    inline fun <reified C: Any, reified S: Any> target(
            noinline targetFunctor: (C) -> S
    ) = ContravariantWithTargetBuilder(
            targetFunctor, typeOf<C>(), typeOf<S>())
}

class ContravariantWithTargetBuilder<C: Any, out S: Any>(
        private val targetFunctor: (C) -> S,
        private val callbackType:  KType,
        private val targetType:    KType
) {
    infix fun rules(
            define: ContravariantPolicyBuilder<C,S>.() -> Unit
    ): ContravariantPolicy {
        val builder = ContravariantPolicyBuilder(
                callbackType, targetType, targetFunctor)
        builder.define()
        return builder.build()
    }
}