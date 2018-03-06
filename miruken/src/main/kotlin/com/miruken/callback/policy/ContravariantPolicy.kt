package com.miruken.callback.policy

import com.miruken.callback.FilteringProvider
import com.miruken.callback.HandleResult
import com.miruken.runtime.isAssignableTo
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

    override fun getKey(callback: Any): Any? =
            super.getKey(callback) ?:
            targetFunctor(callback)?.let {
                when (it) {
                    is KType, is KClass<*> -> it
                    else -> it::class
                }
            } ?: callback::class

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ) = available.filter { key != it && isAssignableTo(it, key) }

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
                ?: if (isAssignableTo(o2, o1)) -1 else 1
    }
    
    private fun compareGenericArity(o1: Any?, o2: Any?) = when (o1) {
        is KType -> when (o2) {
            is KType -> o2.arguments.size -  o1.arguments.size
            is KClass<*> -> o2.typeParameters.size - o1.arguments.size
            is Class<*> -> o2.typeParameters.size - o1.arguments.size
                else -> 0
        }
        is KClass<*> -> when (o2) {
            is KType -> o2.arguments.size - o1.typeParameters.size
            is KClass<*> -> o2.typeParameters.size - o1.typeParameters.size
            is Class<*> -> o2.typeParameters.size -o1.typeParameters.size
            else -> 0
        }
        is Class<*> -> when (o2) {
            is KType -> o2.arguments.size - o1.typeParameters.size
            is Class<*> -> o2.typeParameters.size - o1.typeParameters.size
            else -> 0
        }
        else -> 0
    }
}