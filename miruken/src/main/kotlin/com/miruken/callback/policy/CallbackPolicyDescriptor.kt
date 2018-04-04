package com.miruken.callback.policy

import com.miruken.callback.StringKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
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
        val key = memberBinding.key
        when (key) {
            is KType ->
                if (key.classifier == Any::class)
                    _unknown.add(memberBinding)
                else
                    _typed.getOrPut(key) { mutableListOf() }
                        .add(memberBinding)
            null, Any::class ->
                _unknown.add(memberBinding)
            else ->
                _indexed.getOrPut(key) { mutableListOf() }
                    .add(memberBinding)
        }
    }

    internal fun getInvariantMethods() =
        _typed.values.flatMap { it } + _indexed.values.flatMap { it }

    internal fun getInvariantMethods(
            callback:     Any,
            callbackType: KType?
    ) = policy.getKey(callback, callbackType)?.let {
            when (it) {
                is KType -> _typed[it]
                is String -> _indexed[it] ?: _indexed[StringKey(it)]
                else -> _indexed[it]
            }?.filter { it.approve(callback) }
        } ?: emptyList()

    internal fun getCompatibleMethods(
            callback:     Any,
            callbackType: KType?
    ) = policy.getKey(callback, callbackType)?.let {
            _compatible.getOrPut(it) { inferCompatibleMethods(it) }
        }?.filter { it.approve(callback) } ?: emptyList()

    private fun inferCompatibleMethods(key: Any) =
            when (key) {
                is KType, is KClass<*>, is Class<*> ->
                    policy.getCompatibleKeys(key, _typed.keys).flatMap {
                        _typed[it] ?: emptyList<PolicyMemberBinding>()
                    }
                else ->
                    policy.getCompatibleKeys(key, _indexed.keys).flatMap {
                        _indexed[it] ?: emptyList<PolicyMemberBinding>()
                    }
            }.sortedWith(policy.memberBindingComparator) + _unknown
}