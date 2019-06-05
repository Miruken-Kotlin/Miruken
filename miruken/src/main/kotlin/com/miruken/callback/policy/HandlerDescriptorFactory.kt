package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import kotlin.reflect.KClass

typealias HandlerDescriptorVisitor =
        (HandlerDescriptor, PolicyMemberBinding) -> Unit

interface HandlerDescriptorFactory {
    fun getDescriptor(handlerClass: KClass<*>): HandlerDescriptor?

    fun registerDescriptor(
            handlerClass:  KClass<*>,
            customVisitor: HandlerDescriptorVisitor? = null
    ): HandlerDescriptor

    fun getInstanceHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<HandlerDescriptor>

    fun getTypeHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<HandlerDescriptor>

    fun getCallbackHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null
    ): List<HandlerDescriptor>

    fun getPolicyMembers(policy: CallbackPolicy, key: Any): List<PolicyMemberBinding>

    fun getPolicyMembers(policy: CallbackPolicy): List<PolicyMemberBinding>

    companion object {
        private var factory: HandlerDescriptorFactory? = null
        private val default = MutableHandlerDescriptorFactory()

        val current: HandlerDescriptorFactory
            get() = factory ?: default

        fun useFactory(factory: HandlerDescriptorFactory) {
            this.factory = factory
        }
    }
}

inline fun <reified T> HandlerDescriptorFactory.getDescriptor() =
        getDescriptor(T::class)

inline fun <reified T> HandlerDescriptorFactory.registerDescriptor(
        noinline customVisitor: HandlerDescriptorVisitor? = null
) = registerDescriptor(T::class, customVisitor)

fun HandlerDescriptorFactory.getCallbackMethods(
        policy:       CallbackPolicy,
        callback:     Any,
        callbackType: TypeReference? = null
) = policy.getKey(callback, callbackType)?.let {
    getPolicyMembers(policy)
} ?: emptyList()