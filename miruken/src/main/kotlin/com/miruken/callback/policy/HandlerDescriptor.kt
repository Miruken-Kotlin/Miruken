package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.addSorted
import com.miruken.callback.FilteredObject
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.callback.getFilterProviders
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import com.miruken.runtime.getMetaAnnotations
import com.miruken.runtime.isInstanceCallable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

typealias HandlerDescriptorVisitor =
        (HandlerDescriptor, PolicyMemberBinding) -> Unit

class HandlerDescriptor private constructor(
        val handlerClass: KClass<*>,
        visitor: HandlerDescriptorVisitor? = null
) : FilteredObject() {
    private val _instancePolicies = lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    private val _typePolicies = lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    init {
        validate(handlerClass)
        addCompatibleMembers(visitor)
        addFilters(handlerClass.getFilterProviders())
        handlerClass.companionObject?.also {
            getDescriptor(it, visitor)
        }
    }

    fun getInstancePolicyDescriptor(policy: CallbackPolicy) =
            _instancePolicies.takeIf { it.isInitialized() }?.let {
                it.value[policy]
            }

    fun getTypePolicyDescriptor(policy: CallbackPolicy) =
            _typePolicies.takeIf { it.isInitialized() }?.let {
                it.value[policy]
            }

    internal fun dispatch(
            policy:       CallbackPolicy,
            receiver:     Any,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ): HandleResult {
        var target = receiver
        return when (receiver) {
            is KType -> {
                receiver.jvmErasure.objectInstance?.also {
                    target = it
                }
                _typePolicies
            }
            is KClass<*> -> {
                receiver.objectInstance?.also { target = it }
                _typePolicies
            }
            else -> {
                _instancePolicies.takeUnless {
                    receiver::class.objectInstance != null
                } ?: _typePolicies
            }
        }.takeIf { it.isInitialized() }
                ?.value?.get(policy)?.let {
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
            results:      CollectResultsBlock?
    ) = members.fold(HandleResult.NOT_HANDLED) { result, method ->
        if ((result.handled && !greedy) || result.stop) {
            return result
        }
        result or method.dispatch(receiver, callback,
                callbackType, composer, this, results)
    }

    private fun addCompatibleMembers(visitor: HandlerDescriptorVisitor?) {
        handlerClass.members.filter {
            it.isInstanceCallable }.forEach { member ->
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(member)
            }
            for ((annotation, usePolicies) in member
                    .getMetaAnnotations<UsePolicy>(false)) {
                usePolicies.single().policy?.also {
                    val rule = it.match(dispatch) ?:
                        throw PolicyRejectedException(it, member,
                            "The policy for @${annotation.annotationClass.simpleName} rejected '$member'")
                    val binding  = rule.bind(it, dispatch, annotation)
                    val policies = when {
                        handlerClass.objectInstance != null ->
                            _typePolicies
                        else -> _instancePolicies
                    }
                    val descriptor = policies.value.getOrPut(it) {
                        CallbackPolicyDescriptor(it)
                    }
                    visitor?.invoke(this, binding)
                    descriptor.add(binding)
                }
            }
        }

        handlerClass.constructors.forEach { constructor ->
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(constructor)
            }
            for ((annotation, usePolicies) in constructor
                    .getMetaAnnotations<UsePolicy>(false)) {
                usePolicies.single().policy?.also {
                    val bindingInfo = PolicyMemberBindingInfo(
                            null, dispatch, annotation, false).apply {
                        outKey = constructor.returnType
                    }
                    val binding    = it.bindMethod(bindingInfo)
                    val descriptor = _typePolicies.value.getOrPut(it) {
                        CallbackPolicyDescriptor(it) }
                    visitor?.invoke(this, binding)
                    descriptor.add(binding)
                }
            }
        }
    }

    companion object {
        inline fun <reified T> getDescriptor(
                noinline visitor: HandlerDescriptorVisitor? = null
        ) = getDescriptor(T::class, visitor)

        fun getDescriptor(
                handlerClass: KClass<*>,
                visitor: HandlerDescriptorVisitor? = null
        ) = try {
            DESCRIPTORS.getOrPut(handlerClass) {
                lazy { HandlerDescriptor(handlerClass, visitor) }
            }.value
        } catch (e: Throwable) {
            DESCRIPTORS.remove(handlerClass)
            throw e
        }

        fun resetDescriptors() = DESCRIPTORS.clear()

        fun getInstanceHandlers(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: TypeReference? = null
        ) = getHandlerTypes(policy, callback, callbackType, true, false)

        fun getTypeHandlers(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: TypeReference? = null
        ) = getHandlerTypes(policy, callback, callbackType, false, true)

        fun getCallbackHandlers(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: TypeReference? = null
        ) = getHandlerTypes(policy, callback, callbackType, true, true)

        private fun getHandlerTypes(
                policy:       CallbackPolicy,
                callback:     Any,
                callbackType: TypeReference? = null,
                instances:    Boolean = false,
                types:        Boolean = false
        ): List<KType> {
            if (DESCRIPTORS.isEmpty()) return emptyList()
            val invariants   = mutableListOf<PolicyMemberBinding>()
            val compatible   = mutableListOf<PolicyMemberBinding>()
            val orderMembers = policy.orderMembers

            DESCRIPTORS.values.forEach { handler ->
                val descriptor = handler.value
                val instanceCallbacks = descriptor._instancePolicies.takeIf {
                    instances && it.isInitialized() }?.value?.get(policy)
                val typeCallbacks = descriptor._typePolicies.takeIf {
                    types && it.isInitialized() }?.value?.get(policy)

                (instanceCallbacks
                        ?.getInvariantMembers(callback, callbackType)
                        ?.firstOrNull() ?:
                 typeCallbacks?.getInvariantMembers(callback, callbackType)
                        ?.firstOrNull())?.also { invariants.add(it) } ?:
                (instanceCallbacks
                        ?.getCompatibleMembers(callback, callbackType)
                        ?.firstOrNull() ?:
                 typeCallbacks?.getCompatibleMembers(callback, callbackType)
                        ?.firstOrNull())?.also { compatible.addSorted(it, orderMembers) }
            }

            return (invariants + compatible).map {
                it.dispatcher.owningType
            }
        }

        fun getPolicyMembers(policy: CallbackPolicy, key: Any) =
                DESCRIPTORS.values.flatMap { handler ->
                    val descriptor = handler.value
                    (descriptor._instancePolicies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.let { recv ->
                            recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                        } ?: emptyList()) +
                    (descriptor._typePolicies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.let { recv ->
                            recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                        } ?: emptyList())
                }

        fun getPolicyMembers(policy: CallbackPolicy) =
                DESCRIPTORS.values.flatMap { handler ->
                    val descriptor = handler.value
                    (descriptor._instancePolicies.takeIf { it.isInitialized() }
                        ?.value?.get(policy)?.getInvariantMembers()
                            ?: emptyList()) +
                    (descriptor._typePolicies.takeIf { it.isInitialized() }
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