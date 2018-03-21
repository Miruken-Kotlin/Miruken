package com.miruken.callback.policy

import com.miruken.callback.Callback
import com.miruken.callback.FilteringProvider
import com.miruken.callback.HandleResult
import com.miruken.runtime.isCompatibleWith
import com.miruken.runtime.isUnit
import kotlin.reflect.KClass
import kotlin.reflect.KType

open class ContravariantPolicy(
        rules:   List<MethodRule>,
        filters: List<FilteringProvider>,
        private val targetFunctor: (Any) -> Any?
): CallbackPolicy(rules, filters) {

    constructor(
            build: ContravariantTargetBuilder.() -> ContravariantPolicy
    ) : this(ContravariantTargetBuilder().build())

    constructor(prototype: ContravariantPolicy) : this(
            prototype.rules, prototype.filters, prototype.targetFunctor
    )

    override fun getKey(callback: Any, callbackType: KType?): Any? =
            (callback as? Callback)?.getCallbackKey() ?:
            targetFunctor(callback)?.let {
                when (it) {
                    is KType, is KClass<*> -> it
                    else -> it::class
                }
            } ?: callbackType ?: callback::class

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ) = available.filter { key != it && isCompatibleWith(it, key) }

    override fun acceptResult(result: Any?, binding: PolicyMethodBinding) =
            when (result) {
                null, Unit ->
                    if (binding.dispatcher.returnType.isUnit)
                        HandleResult.HANDLED else HandleResult.NOT_HANDLED
                is HandleResult -> result
                else -> HandleResult.HANDLED
            }

    override fun compare(o1: Any?, o2: Any?) = when {
        o1 == o2 -> 0
        o1 == null -> 1
        o2 == null -> -1
        else -> compareGenericArity(o1, o2).takeIf { it != 0 }
                ?: if (isCompatibleWith(o2, o1)) -1 else 1
    }
}