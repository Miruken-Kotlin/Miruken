package com.miruken.callback.policy

import com.miruken.Initializing
import com.miruken.TypeReference
import com.miruken.addSorted
import com.miruken.callback.GenericWrapper
import com.miruken.callback.InitializeProvider
import com.miruken.callback.Provider
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import com.miruken.runtime.getMetaAnnotations
import com.miruken.runtime.isInstanceCallable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf

class MutableHandlerDescriptorFactory(
        private val visitor: HandlerDescriptorVisitor? = null
) : HandlerDescriptorFactory {

    private val _descriptors =
            ConcurrentHashMap<KClass<*>, Lazy<HandlerDescriptor>>()

    init { registerDefaults() }

    override fun getDescriptor(handlerClass: KClass<*>) =
        _descriptors[handlerClass]?.value

    override fun registerDescriptor(
            handlerClass:  KClass<*>,
            customVisitor: HandlerDescriptorVisitor?
    ): HandlerDescriptor = try {
        _descriptors.getOrPut(handlerClass) {
            lazy { createDescriptor(handlerClass, customVisitor) }
        }.value.also {
            handlerClass.companionObject?.run {
                registerDescriptor(this, customVisitor)
            }
        }
    } catch (e: Throwable) {
        _descriptors.remove(handlerClass)
        throw e
    }

    override fun getInstanceHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?
    ) = getDescriptors(policy, callback, callbackType, instances = true, types = false)

    override fun getTypeHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?
    ) = getDescriptors(policy, callback, callbackType, instances = false, types = true)

    override fun getCallbackHandlers(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?
    ) = getDescriptors(policy, callback, callbackType, instances = true, types = true)

    private fun getDescriptors(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference? = null,
            instances:    Boolean = false,
            types:        Boolean = false
    ): List<HandlerDescriptor> {
        if (_descriptors.isEmpty()) return emptyList()
        val invariants   = mutableListOf<PolicyMemberBinding>()
        val compatible   = mutableListOf<PolicyMemberBinding>()
        val orderMembers = policy.orderMembers

        _descriptors.values.forEach { handler ->
            val descriptor        = handler.value
            val instanceCallbacks = descriptor.instancePolicies
                    .takeIf { instances }?.get(policy)
            val typeCallbacks     = descriptor.typePolicies
                    .takeIf { types }?.get(policy)

            (instanceCallbacks
                    ?.getInvariantMembers(callback, callbackType)
                    ?.firstOrNull() ?:
            typeCallbacks?.getInvariantMembers(callback, callbackType)
                    ?.firstOrNull())?.also { invariants.add(it) } ?:
            (instanceCallbacks
                    ?.getCompatibleMembers(callback, callbackType)
                    ?.firstOrNull() ?:
            typeCallbacks?.getCompatibleMembers(callback, callbackType)
                    ?.firstOrNull())?.also {
                compatible.addSorted(it, orderMembers)
            }
        }

        return (invariants + compatible).mapNotNull {
            getDescriptor(it.dispatcher.owningClass)
        }
    }

    override fun getPolicyMembers(policy: CallbackPolicy, key: Any) =
            _descriptors.values.flatMap { handler ->
                val descriptor = handler.value
                (descriptor.instancePolicies[policy]?.let { recv ->
                    recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                } ?: emptyList()) +
                (descriptor.typePolicies[policy]?.let { recv ->
                    recv.getInvariantMembers(key, null) +
                            recv.getCompatibleMembers(key, null)
                } ?: emptyList())
            }

    override fun getPolicyMembers(policy: CallbackPolicy) =
            _descriptors.values.flatMap { handler ->
                val descriptor = handler.value
                (descriptor.instancePolicies[policy]?.invariantMembers
                        ?: emptyList()) +
                (descriptor.typePolicies[policy]?.invariantMembers
                        ?: emptyList())
            }

    private fun createDescriptor(
            handlerClass:  KClass<*>,
            customVisitor: HandlerDescriptorVisitor?
    ): HandlerDescriptor {
        validate(handlerClass)
        var instancePolicies: MutableMap<CallbackPolicy, MutableList<PolicyMemberBinding>>? = null
        var typePolicies:     MutableMap<CallbackPolicy, MutableList<PolicyMemberBinding>>? = null
        handlerClass.members.filter {
            it.isInstanceCallable }.forEach { member ->
            val method = when (member) {
                is KProperty<*> -> member.getter
                is KFunction<*> -> member
                else -> null
            } ?: return@forEach
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(method)
            }
            for ((annotation, usePolicies) in method
                    .getMetaAnnotations<UsePolicy>(false)) {
                usePolicies.single().policy?.also {
                    val rule = it.match(dispatch) ?:
                    throw PolicyRejectedException(it, method,
                            "The policy for @${annotation.annotationClass.simpleName} rejected '$member'")
                    val binding  = rule.bind(it, dispatch, annotation)
                    val policies = when {
                        handlerClass.objectInstance != null -> {
                            if (typePolicies == null) {
                                typePolicies = mutableMapOf()
                            }
                            typePolicies!!
                        }
                        else -> {
                            if (instancePolicies == null) {
                                instancePolicies = mutableMapOf()
                            }
                            instancePolicies!!
                        }
                    }
                    policies.getOrPut(it) { mutableListOf() }.add(binding)
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
                    val binding = it.bindMethod(bindingInfo)
                    if (handlerClass.isSubclassOf(Initializing::class)) {
                        binding.addFilters(InitializeProvider)
                    }
                    if (typePolicies == null) {
                        typePolicies = mutableMapOf()
                    }
                    typePolicies!!.getOrPut(it) { mutableListOf() }.add(binding)
                }
            }
        }

        val descriptor = HandlerDescriptor(
                handlerClass,
                instancePolicies?.mapValues { entry  ->
                    CallbackPolicyDescriptor(entry.key, entry.value)
                } ?: emptyMap(),
                typePolicies?.mapValues { entry  ->
                    CallbackPolicyDescriptor(entry.key, entry.value)
                } ?: emptyMap()
        )

        if (visitor != null || customVisitor != null) {
            instancePolicies?.values?.flatten()?.forEach {
                visitor?.invoke(descriptor, it)
                customVisitor?.invoke(descriptor, it)
            }
            typePolicies?.values?.flatten()?.forEach {
                visitor?.invoke(descriptor, it)
                customVisitor?.invoke(descriptor, it)
            }
        }

        return descriptor
    }

    private fun validate(handlerClass: KClass<*>) {
        val javaClass = handlerClass.java
        check(!javaClass.isInterface) {
            "Handlers cannot be interfaces: ${handlerClass.qualifiedName}"
        }
        check(handlerClass.javaPrimitiveType == null) {
            "Handlers cannot be primitive types: ${handlerClass.qualifiedName}"
        }
        check(!javaClass.isArray &&
              !handlerClass.isSubclassOf(Collection::class)) {
            "Handlers cannot be collections or arrays: ${handlerClass.qualifiedName}"
        }
    }

    private fun registerDefaults() {
        registerDescriptor<Provider>()
        registerDescriptor<GenericWrapper>()
    }
}
