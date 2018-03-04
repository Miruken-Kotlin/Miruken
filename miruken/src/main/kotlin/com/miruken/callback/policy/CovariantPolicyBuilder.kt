package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.typeOf
import kotlin.reflect.KType

class CovariantPolicyBuilder<C: Any>(
        callbackType: KType,
        private val keyFunctor: (C) -> Any
) : CallbackPolicyBuilder(callbackType) {
    val key  = ReturnsKey
    val unit = ReturnsUnit

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): CovariantPolicy {
        return CovariantPolicy(rules, filters, {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                keyFunctor(it as C) else null
        })
    }
}

class CovariantKeyBuilder {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = CovariantWithKeyBuilder(keyFunctor, typeOf<C>())
}

class CovariantWithKeyBuilder<C: Any>(
        private val keyFunctor:   (C) -> Any,
        private val callbackType: KType
) {
    infix fun rules(
            define: CovariantPolicyBuilder<C>.() -> Unit
    ): CovariantPolicy {
        val builder = CovariantPolicyBuilder(callbackType, keyFunctor)
        define(builder)
        return builder.build()
    }
}