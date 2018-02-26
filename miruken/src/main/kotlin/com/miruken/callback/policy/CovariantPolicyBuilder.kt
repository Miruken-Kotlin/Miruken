package com.miruken.callback.policy

import com.miruken.runtime.typeOf
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class CovariantPolicyBuilder<C: Any>(
        policy:       CovariantPolicy,
        keyFunctor:   (C) -> Any,
        callbackType: KType
) : CallbackPolicyBuilder(policy, callbackType) {

    init {
        policy.keyFunctor = {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                keyFunctor(it as C) else null
        }
    }

    val key  = ReturnsKey
    val unit = ReturnsUnit

    inline fun <reified E: Any> extract (noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)
}

class CovariantKeyBuilder(val policy: CovariantPolicy) {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = CovariantWithKeyBuilder(policy, keyFunctor,  typeOf<C>())
}

@Suppress("MemberVisibilityCanBePrivate")
class CovariantWithKeyBuilder<C: Any>(
        val policy:       CovariantPolicy,
        val keyFunctor:   (C) -> Any,
        val callbackType: KType
) {
    inline infix fun rules(build: CovariantPolicyBuilder<C>.() -> Unit) {
        val builder = CovariantPolicyBuilder(policy, keyFunctor, callbackType)
        builder.build()
    }
}