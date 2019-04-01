package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.FilteredObject
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

class HandlerDescriptor(
        val handlerClass:     KClass<*>,
        val instancePolicies: Map<CallbackPolicy, CallbackPolicyDescriptor>,
        val typePolicies:     Map<CallbackPolicy, CallbackPolicyDescriptor>
) : FilteredObject(handlerClass) {

    fun getInstancePolicyDescriptor(policy: CallbackPolicy) =
            instancePolicies[policy]

    fun getTypePolicyDescriptor(policy: CallbackPolicy) =
            typePolicies[policy]

    fun dispatch(
            policy:       CallbackPolicy,
            receiver:     Any,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock
    ): HandleResult {
        var target = receiver
        return when (receiver) {
            is KType -> {
                receiver.jvmErasure.objectInstance?.also {
                    target = it
                }
                typePolicies
            }
            is KClass<*> -> {
                receiver.objectInstance?.also { target = it }
                typePolicies
            }
            else -> {
                instancePolicies.takeUnless {
                    receiver::class.objectInstance != null
                } ?: typePolicies
            }
        }[policy]?.let {
            dispatch(it.getInvariantMembers(callback, callbackType),
                    target, callback, callbackType,
                    greedy, composer, results).otherwise(greedy) {
            dispatch(it.getCompatibleMembers(callback, callbackType),
                    target, callback, callbackType,
                    greedy, composer, results)
            }
        } ?: HandleResult.NOT_HANDLED
    }

    private fun dispatch(
            members:      Collection<PolicyMemberBinding>,
            receiver:     Any,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock
    ) = members.fold(HandleResult.NOT_HANDLED) { result, method ->
        if ((result.handled && !greedy) || result.stop) {
            return result
        }
        result or method.dispatch(receiver, callback,
                callbackType, composer, this, results)
    }
}