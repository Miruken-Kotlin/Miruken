package com.miruken.callback.policy

import com.miruken.runtime.ANY_TYPE
import java.util.concurrent.ConcurrentHashMap
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
            null, ANY_TYPE -> _unknown.add(methodBinding)
            is KType ->
                _typed.getOrPut(key) { mutableListOf(methodBinding) }
            else ->
                _indexed.getOrPut(key) { mutableListOf(methodBinding) }
        }
    }

    internal fun getInvariantMethods() =
        _typed.values.flatMap { it } + _indexed.values.flatMap { it }

    internal fun getInvariantMethods(callback: Any) =
        policy.getKey(callback)?.let {
            when (it) {
                is KType -> _typed[it]
                else -> _indexed[it]
            }?.filter { it.approves(callback) }
        } ?: emptyList()

    internal fun getCompatibleMethods(callback: Any) =
        policy.getKey(callback)?.let {
            _compatible.getOrPut(it) { inferCompatibleMethods(it) }
        }?.filter { it.approves(callback) } ?: emptyList()

    internal fun inferCompatibleMethods(key: Any): List<PolicyMethodBinding> {
        return when (key) {
            is KType ->
                policy.getCompatibleKeys(key, _typed.keys).flatMap {
                    _typed[it] ?: emptyList<PolicyMethodBinding>()
                }
            else ->
                policy.getCompatibleKeys(key, _indexed.keys).flatMap {
                    _indexed[it] ?: emptyList<PolicyMethodBinding>()
                }
        }.sortedWith(policy) + _unknown
    }
}