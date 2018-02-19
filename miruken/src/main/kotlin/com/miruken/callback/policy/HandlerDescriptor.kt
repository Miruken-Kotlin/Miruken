package com.miruken.callback.policy

import com.miruken.runtime.getTaggedAnnotations
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class HandlerDescriptor(val handlerClass: KClass<*>) {

    private val _policies by lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    init {
        validateHandlerClass(handlerClass)
        findCompatibleMembers()
    }

    private fun findCompatibleMembers() {
        for (member in handlerClass.members) {
            val usesPolicies = member.getTaggedAnnotations<UsePolicy<*>>()
            if (usesPolicies.isNotEmpty()) {

            }
        }
    }

    companion object {
        inline fun <reified T> getDescriptor() = getDescriptor(T::class)

        fun getDescriptor(handlerClass: KClass<*>) : HandlerDescriptor {
            try {
                return DESCRIPTORS.getOrPut(handlerClass) {
                    lazy { HandlerDescriptor(handlerClass) }
                }.value
            } catch (e: Throwable) {
                DESCRIPTORS.remove(handlerClass)
                throw e
            }
        }

        fun resetDescriptors() = DESCRIPTORS.clear()

        private fun validateHandlerClass(handlerClass: KClass<*>) {
            val javaClass = handlerClass.java
            if (javaClass.isInterface) {
                throw IllegalArgumentException(
                        "Handlers cannot be interfaces: ${handlerClass.qualifiedName}")
            }
            if (handlerClass.isAbstract) {
                throw IllegalArgumentException(
                        "Handlers cannot be abstract classes: ${handlerClass.qualifiedName}")
            }
            if (handlerClass.javaPrimitiveType != null) {
                throw IllegalArgumentException(
                        "Handlers cannot be primitive types: ${handlerClass.qualifiedName}")
            }
            if (handlerClass.isSubclassOf(Collection::class)) {
                throw IllegalArgumentException(
                        "Handlers cannot be collection classes: ${handlerClass.qualifiedName}")
            }
        }

        private val DESCRIPTORS =
                ConcurrentHashMap<KClass<*>, Lazy<HandlerDescriptor>>()
    }
}