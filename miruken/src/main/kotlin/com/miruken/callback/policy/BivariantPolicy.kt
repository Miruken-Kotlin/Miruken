package com.miruken.callback.policy

import com.miruken.callback.Callback
import com.miruken.callback.FilteringProvider
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

open class BivariantPolicy(
        rules:   List<MethodRule>,
        filters: List<FilteringProvider>,
        val output: CovariantPolicy,
        val input:  ContravariantPolicy
) : CallbackPolicy(rules, filters, true) {

    constructor(
            build: BivariantKeyBuilder.() -> BivariantPolicy
    ) : this(BivariantKeyBuilder().build())

    constructor(prototype: BivariantPolicy) : this(
            prototype.rules, prototype.filters,
            prototype.output, prototype.input)

    override fun createKey(
            bindingInfo: PolicyMemberBindingInfo
    ): Pair<Any, Any>? {
        val inKey  = bindingInfo.inKey
        val outKey = bindingInfo.outKey
        return if (inKey != null && outKey != null)
            outKey to inKey else null
    }

    override fun getKey(callback: Any, callbackType: KType?): Any? =
            (callback as? Callback)?.getCallbackKey()
                    ?: output.getKey(callback, callbackType) to
                       input.getKey(callback, callbackType)

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any> {
        if (key !is Pair<*,*>) return emptyList()
        val (outputKey, inputKey) = key
        val compatible = mutableListOf<Pair<*,*>>()
        available.filterIsInstance<Pair<Any,Any>>().forEach {
            val (testOutKey, testInKey) = it
            val inEqual  = isCompatibleWith(testInKey, inputKey!!)
            val outEqual = isCompatibleWith(testOutKey, outputKey!!)
            if (!outEqual && output.getCompatibleKeys(
                 outputKey, listOf(testOutKey)).isEmpty()) {
                return@forEach
            }
            if (!inEqual && input.getCompatibleKeys(
                 inputKey, listOf(testInKey)).isEmpty()) {
                return@forEach
            }
            compatible.add(it)
        }
        return compatible
    }

    override fun acceptResult(result: Any?, binding: PolicyMemberBinding) =
        output.acceptResult(result, binding)

    override fun compare(o1: Any?, o2: Any?): Int {
        val pair1 = o1 as? Pair<*,*>
        val pair2 = o2 as? Pair<*,*>
        val order = input.compare(pair1?.second, pair2?.second)
        return order.takeUnless { it == 0 }
            ?: output.compare(pair1?.first, pair2?.first)
    }
}