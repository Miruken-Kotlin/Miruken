package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType

typealias HandlerDescriptorVisitor =
        (HandlerDescriptor, PolicyMemberBinding) -> Unit

interface HandlerDescriptorFactory {
    fun getDescriptor(handlerClass: KClass<*>): HandlerDescriptor?

    fun getInstanceHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<KType>

    fun getTypeHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<KType>

    fun getCallbackHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<KType>

    fun getPolicyMembers(policy: CallbackPolicy, key: Any): List<PolicyMemberBinding>

    fun getPolicyMembers(policy: CallbackPolicy): List<PolicyMemberBinding>

    companion object {
        private var currentFactory: HandlerDescriptorFactory? = null

        var current: HandlerDescriptorFactory
            get() = currentFactory ?: LazyHandlerDescriptorFactory.DEFAULT
            set(value) { currentFactory = value }
    }
}

inline fun <reified T> HandlerDescriptorFactory.getDescriptor() =
        getDescriptor(T::class)

fun HandlerDescriptorFactory.getCallbackMethods(
        policy:       CallbackPolicy,
        callback:     Any,
        callbackType: TypeReference? = null
) = policy.getKey(callback, callbackType)?.let {
    getPolicyMembers(policy)
} ?: emptyList()