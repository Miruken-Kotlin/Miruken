package com.miruken.callback.policy

import com.miruken.callback.StringKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType

class CallbackPolicyDescriptor(val policy: CallbackPolicy) {
    private val _typed =
            HashMap<KType, MutableList<PolicyMethodBinding>>()

    private val _compatible =
            ConcurrentHashMap<Any, List<PolicyMethodBinding>>()

    private val _indexed by lazy {
        HashMap<Any, MutableList<PolicyMethodBinding>>()
    }

    private val _unknown by lazy {
        mutableListOf<PolicyMethodBinding>()
    }

    internal fun add(methodBinding: PolicyMethodBinding) {
        val key = methodBinding.key
        when (key) {
            is KType ->
                if (key.classifier == Any::class)
                    _unknown.add(methodBinding)
                else
                    _typed.getOrPut(key) { mutableListOf() }
                        .add(methodBinding)
            null, Any::class ->
                _unknown.add(methodBinding)
            else ->
                _indexed.getOrPut(key) { mutableListOf() }
                    .add(methodBinding)
        }
    }

    internal fun getInvariantMethods() =
        _typed.values.flatMap { it } + _indexed.values.flatMap { it }

    internal fun getInvariantMethods(callback: Any) =
        policy.getKey(callback)?.let {
            when (it) {
                is KType -> _typed[it]
                is String -> _indexed[it] ?: _indexed[StringKey(it)]
                else -> _indexed[it]
            }?.filter { it.approve(callback) }
        } ?: emptyList()

    internal fun getCompatibleMethods(callback: Any) =
        policy.getKey(callback)?.let {
            _compatible.getOrPut(it) { inferCompatibleMethods(it) }
        }?.filter { it.approve(callback) } ?: emptyList()

    private fun inferCompatibleMethods(key: Any): List<PolicyMethodBinding> {
        return when (key) {
            is KType, is KClass<*>, is Class<*> ->
                policy.getCompatibleKeys(key, _typed.keys).flatMap {
                    _typed[it] ?: emptyList<PolicyMethodBinding>()
                }
            else ->
                policy.getCompatibleKeys(key, _indexed.keys).flatMap {
                    _indexed[it] ?: emptyList<PolicyMethodBinding>()
                }
        }.sortedWith(policy.methodBindingComparator) + _unknown
    }
}