package com.miruken.callback.policy

import com.miruken.callback.Callback
import com.miruken.callback.FilteringProvider
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

open class CovariantPolicy(
        rules:   List<MethodRule>,
        filters: Collection<FilteringProvider>,
        private val keyFunctor: (Any) -> Any?
) : CallbackPolicy(rules, filters) {

    constructor(
            build: CovariantKeyBuilder.() -> CovariantPolicy
    ) : this(CovariantKeyBuilder().build())

    constructor(prototype: CovariantPolicy) : this(
            prototype.rules, prototype.filters, prototype.keyFunctor
    )

    override fun getKey(callback: Any, callbackType: KType?): Any? =
            (callback as? Callback)?.getCallbackKey()
                    ?: keyFunctor(callback)
                    ?: callbackType

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ) = available.filter { key != it && isCompatibleWith(key, it) }

    override fun getResultType(callback: Any) =
            keyFunctor(callback) as? KType?

    override fun compare(o1: Any?, o2: Any?) = when {
        o1 == o2 -> 0
        o1 == null -> 1
        o2 == null -> -1
        else -> compareGenericArity(o1, o2).takeIf { it != 0 }
                ?: if (isCompatibleWith(o1, o2)) -1 else 1
    }
}