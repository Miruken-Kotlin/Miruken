package com.miruken.callback.policy

import com.miruken.runtime.getKType
import kotlin.reflect.KType

class CovariantPolicyBuilder<C: Any>(
        policy:       CovariantPolicy,
        keyFunctor:   (C) -> Any,
        callbackType: KType
) : CallbackPolicyBuilder(policy, callbackType) {

    val returnKey = ReturnsKey

    inline fun <reified E: Any> extract (noinline block: (C) -> E) =
            ExtractArgument(getKType<E>(), block)
}

class CovariantTargetBuilder(val policy: CovariantPolicy) {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any,
            build: CovariantPolicyBuilder<C>.() -> Unit
    ) {
        val builder = CovariantPolicyBuilder(
                policy, keyFunctor, getKType<C>())
        builder.build()
    }
}