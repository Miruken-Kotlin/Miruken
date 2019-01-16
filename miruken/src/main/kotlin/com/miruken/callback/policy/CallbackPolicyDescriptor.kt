package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.addSorted
import com.miruken.callback.StringKey
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

class CallbackPolicyDescriptor(
        val policy: CallbackPolicy,
        bindings:   Collection<PolicyMemberBinding>
) {
    private val _typed      = HashMap<KType, MutableList<PolicyMemberBinding>>()
    private val _compatible = ConcurrentHashMap<Any, List<PolicyMemberBinding>>()

    private val _indexed by lazy { HashMap<Any, MutableList<PolicyMemberBinding>>() }
    private val _unknown by lazy { mutableListOf<PolicyMemberBinding>() }

    init { bindings.forEach(::addBinding) }

    val invariantMembers by lazy {
        _typed.values.flatten() + _indexed.values.flatten()
    }

    fun getInvariantMembers(
            callback:     Any,
            callbackType: TypeReference?
    ) = policy.getKey(callback, callbackType)?.let { key ->
        when (val type = TypeReference.getKType(key) ?: key) {
                is KType -> _typed[type]
                is String -> _indexed[type] ?: _indexed[StringKey(type)]
                else -> _indexed[key]
            }?.filter { it.approve(callback) }
        } ?: emptyList()

    fun getCompatibleMembers(
            callback:     Any,
            callbackType: TypeReference?
    ) = policy.getKey(callback, callbackType)?.let {
            _compatible.getOrPut(it) { inferCompatibleMembers(it) }
        }?.filter { it.approve(callback) } ?: emptyList()

    private fun inferCompatibleMembers(key: Any) =
        when (val type = TypeReference.getKType(key)) {
            is KType ->
                policy.getCompatibleKeys(type, _typed.keys).flatMap {
                    _typed[it] ?: emptyList<PolicyMemberBinding>()
                }
            else ->
                policy.getCompatibleKeys(key, _indexed.keys).flatMap {
                    _indexed[it] ?: emptyList<PolicyMemberBinding>()
                }
        }.sortedWith(policy.orderMembers) + _unknown

    private fun addBinding(memberBinding: PolicyMemberBinding) {
        val key  = memberBinding.key
        val list = when (val type = TypeReference.getKType(key) ?: key) {
            is KType -> if (type.classifier == Any::class) {
                _unknown
            } else {
                _typed.getOrPut(type) { mutableListOf() }
            }
            null -> _unknown
            else -> _indexed.getOrPut(key!!) { mutableListOf() }
        }
        list.addSorted(memberBinding, PolicyMemberBinding)
    }
}