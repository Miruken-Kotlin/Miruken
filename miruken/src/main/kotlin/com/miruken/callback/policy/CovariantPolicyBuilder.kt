package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.policy.rules.ExtractArgument
import com.miruken.callback.policy.rules.ReturnsKey
import com.miruken.callback.policy.rules.ReturnsUnit
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf

@CalbackPolicyDsl
class CovariantPolicyBuilder<C: Any>(
        callbackType: TypeReference,
        private val keyFunctor: (C) -> Any
) : CallbackPolicyBuilder(callbackType) {
    val key  = ReturnsKey
    val unit = ReturnsUnit

    inline fun <reified E: Any> extract(noinline block: C.() -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): CovariantPolicy {
        return CovariantPolicy(rules, filters) {
            @Suppress("UNCHECKED_CAST")
            if (isCompatibleWith(callbackType, it))
                keyFunctor(it as C) else null
        }
    }
}

@CalbackPolicyDsl
class CovariantKeyBuilder {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = CovariantWithKeyBuilder(keyFunctor, typeOf<C>())
}

@CalbackPolicyDsl
class CovariantWithKeyBuilder<C: Any>(
        private val keyFunctor:   (C) -> Any,
        private val callbackType: TypeReference
) {
    infix fun rules(
            define: CovariantPolicyBuilder<C>.() -> Unit
    ): CovariantPolicy {
        val builder = CovariantPolicyBuilder(callbackType, keyFunctor)
        define(builder)
        return builder.build()
    }
}