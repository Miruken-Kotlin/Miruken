package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.addSorted
import com.miruken.callback.StringKey
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

class CallbackPolicyDescriptor(val policy: CallbackPolicy) {
    private val _typed =
            HashMap<KType, MutableList<PolicyMemberBinding>>()

    private val _compatible =
            ConcurrentHashMap<Any, List<PolicyMemberBinding>>()

    private val _indexed by lazy {
        HashMap<Any, MutableList<PolicyMemberBinding>>()
    }

    private val _unknown by lazy {
        mutableListOf<PolicyMemberBinding>()
    }

    internal fun add(memberBinding: PolicyMemberBinding) {
        val list = when (val key = memberBinding.key) {
            is KType ->
                if (key.classifier == Any::class)
                    _unknown
                else
                    _typed.getOrPut(key) { mutableListOf() }
            null, Any::class -> _unknown
            else -> _indexed.getOrPut(key) { mutableListOf() }
        }
        list.addSorted(memberBinding, PolicyMemberBinding)
    }

    fun getInvariantMembers() =
        _typed.values.flatten() + _indexed.values.flatten()

    fun getInvariantMembers(
            callback:     Any,
            callbackType: TypeReference?
    ) = policy.getKey(callback, callbackType)?.let { key ->
        when (key) {
                is KType -> _typed[key]
                is TypeReference -> _typed[key.kotlinType]
                is String -> _indexed[key] ?: _indexed[StringKey(key)]
                else -> _indexed[key]
            }?.filter { it.approve(callback) }
        } ?: emptyList()

    fun getCompatibleMembers(
            callback:     Any,
            callbackType: TypeReference?
    ) = policy.getKey(callback, callbackType)?.let {
            _compatible.getOrPut(it) { inferCompatibleMembers(it) }
        }?.filter { it.approve(callback) } ?: emptyList()

    // TODO:  For now all keys will be KType until we transition to TypeReference

    private fun inferCompatibleMembers(key: Any) =
            when (key) {
                is KType ->
                    policy.getCompatibleKeys(key, _typed.keys).flatMap {
                        _typed[it] ?: emptyList<PolicyMemberBinding>()
                    }
                is TypeReference ->
                    policy.getCompatibleKeys(key.kotlinType, _typed.keys).flatMap {
                        _typed[it] ?: emptyList<PolicyMemberBinding>()
                    }
                else ->
                    policy.getCompatibleKeys(key, _indexed.keys).flatMap {
                        _indexed[it] ?: emptyList<PolicyMemberBinding>()
                    }
            }.sortedWith(policy.orderMembers) + _unknown
}