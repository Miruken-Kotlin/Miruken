package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.runtime.getTaggedAnnotations
import com.miruken.runtime.isInstanceCallable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

class HandlerDescriptor(val handlerClass: KClass<*>) {
    private val _policies = lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    private val _noReceiverpolicies = lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    init {
        validate(handlerClass)
        findCompatibleMembers()
    }

    internal fun dispatch(
            policy:       CallbackPolicy,
            receiver:     Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ) = when (receiver) {
            is KType, is KClass<*> -> _noReceiverpolicies
            else -> _policies
        }.takeIf { it.isInitialized() }?.value?.get(policy)?.let {
            dispatch(it.getInvariantMembers(callback, callbackType),
                    receiver, callback, callbackType,
                    greedy, composer, results).otherwise(greedy) {
            dispatch(it.getCompatibleMembers(callback, callbackType),
                    receiver, callback, callbackType,
                    greedy, composer, results)
            }
        } ?: HandleResult.NOT_HANDLED

    private fun dispatch(
            members:      Collection<PolicyMemberBinding>,
            receiver:     Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock?
    ) = members.fold(HandleResult.NOT_HANDLED) { result, method ->
        if ((result.handled && !greedy) || result.stop) {
            return result
        }
        result or method.dispatch(receiver, callback,
                callbackType, composer, results)
    }

    private fun findCompatibleMembers() {
        handlerClass.members.filter {
            it.isInstanceCallable }.forEach { member ->
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(member)
            }
            for ((annotation, usePolicies) in member
                    .getTaggedAnnotations<UsePolicy>()) {
                usePolicies.single().policy?.also {
                    val rule = it.match(dispatch) ?:
                        throw PolicyRejectedException(it, member,
                                "The policy for @${annotation.annotationClass.simpleName} rejected '$member'")
                    val binding    = rule.bind(it, dispatch, annotation)
                    val descriptor = _policies.value.getOrPut(it) {
                        CallbackPolicyDescriptor(it) }
                    descriptor.add(binding)
                }
            }
        }

        handlerClass.constructors.forEach { constructor ->
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(constructor)
            }
            for ((annotation, usePolicies) in constructor
                    .getTaggedAnnotations<UsePolicy>()) {
                usePolicies.single().policy?.also {
                    val bindingInfo = PolicyMemberBindingInfo(
                            null, dispatch, annotation, false).apply {
                        outKey = constructor.returnType
                    }
                    val descriptor = _noReceiverpolicies.value.getOrPut(it) {
                        CallbackPolicyDescriptor(it) }
                    descriptor.add(it.bindMethod(bindingInfo))
                }
            }
        }
    }

    companion object {
        inline fun <reified T> getDescriptor() =
                getDescriptor(T::class)

        fun getDescriptor(handlerClass: KClass<*>) =
                try {
                    DESCRIPTORS.getOrPut(handlerClass) {
                        lazy { HandlerDescriptor(handlerClass) }
                    }.value
                } catch (e: Throwable) {
                    DESCRIPTORS.remove(handlerClass)
                    throw e
                }

        fun resetDescriptors() = DESCRIPTORS.clear()

        fun getInstanceHandlers(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: KType? = null
        ) = getHandlerTypes(policy, callback, callbackType, true, false)

        fun getNoReceiverHandlers(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: KType? = null
        ) = getHandlerTypes(policy, callback, callbackType, false, true)

        fun getHandlerTypes(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: KType? = null
        ) = getHandlerTypes(policy, callback, callbackType, true, true)

        private fun getHandlerTypes(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: KType?  = null,
                receiver:     Boolean = false,
                noReceiver:   Boolean = false
        ) = DESCRIPTORS.values.mapNotNull { handler ->
            val descriptor = handler.value
            descriptor._policies.takeIf { receiver && it.isInitialized() }
                    ?.value?.get(policy)?.let { recv ->
                    recv.getInvariantMembers(callback, callbackType)
                            .firstOrNull() ?:
                    recv.getCompatibleMembers(callback, callbackType)
                            .firstOrNull()
                } ?:
            descriptor._noReceiverpolicies.takeIf {
                noReceiver && it.isInitialized() }
                    ?.value?.get(policy)?.let { recv ->
                    recv.getInvariantMembers(callback, callbackType)
                            .firstOrNull() ?:
                    recv.getCompatibleMembers(callback, callbackType)
                            .firstOrNull()
             }
        }.sortedWith(policy.memberBindingComparator)
                    .map {
                        it.dispatcher.owningType
                    }
                    .distinct()

        fun getPolicyMembers(policy: CallbackPolicy, key: Any) =
                DESCRIPTORS.values.flatMap { handler ->
                    val descriptor = handler.value
                    (descriptor._policies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.let { recv ->
                            recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                        } ?: emptyList()) +
                    (descriptor._noReceiverpolicies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.let { recv ->
                            recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                        } ?: emptyList())
                }

        fun getPolicyMembers(policy: CallbackPolicy) =
                DESCRIPTORS.values.flatMap { handler ->
                    val descriptor = handler.value
                    (descriptor._policies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.getInvariantMembers()
                            ?: emptyList()) +
                    (descriptor._noReceiverpolicies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.getInvariantMembers()
                            ?: emptyList())
                }

        private fun validate(handlerClass: KClass<*>) {
            val javaClass = handlerClass.java
            check(!javaClass.isInterface) {
                "Handlers cannot be interfaces: ${handlerClass.qualifiedName}"
            }
            check(!handlerClass.isAbstract) {
                "Handlers cannot be abstract classes: ${handlerClass.qualifiedName}"
            }
            check(handlerClass.javaPrimitiveType == null) {
                "Handlers cannot be primitive types: ${handlerClass.qualifiedName}"
            }
            check(!javaClass.isArray &&
                  !handlerClass.isSubclassOf(Collection::class)) {
                "Handlers cannot be collections or arrays: ${handlerClass.qualifiedName}"
            }
        }

        private val DESCRIPTORS =
                ConcurrentHashMap<KClass<*>, Lazy<HandlerDescriptor>>()
    }
}