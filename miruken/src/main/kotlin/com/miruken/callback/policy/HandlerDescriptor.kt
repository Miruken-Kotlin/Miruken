package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.runtime.getTaggedAnnotations
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

class HandlerDescriptor(val handlerClass: KClass<*>) {

    private val _policies by lazy {
        HashMap<CallbackPolicy, CallbackPolicyDescriptor>()
    }

    init {
        validateHandlerClass(handlerClass)
        findCompatibleMembers()
    }

    internal fun dispatch(
            policy:   CallbackPolicy,
            receiver: Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ): HandleResult {
        return _policies[policy]?.let {
            dispatch(it.getInvariantMethods(callback),
                    receiver, callback, greedy, composer, results
                   ).otherwise(greedy) {
            dispatch(it.getCompatibleMethods(callback),
                    receiver, callback, greedy, composer, results)
            }
        } ?: HandleResult.NOT_HANDLED
    }

    private fun dispatch(
            methods:  Collection<PolicyMethodBinding>,
            receiver: Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock?
    ) : HandleResult =
            methods.fold(HandleResult.NOT_HANDLED, { result, method ->
                if ((result.handled && !greedy) || result.stop) {
                    return result
                }
                result or method.dispatch(
                        receiver, callback, composer, results)
            })

    private fun findCompatibleMembers() {
        handlerClass.members.filter(::isInstanceMethod).forEach { member ->
            val dispatch by lazy(LazyThreadSafetyMode.NONE) {
                CallableDispatch(member)
            }
            for ((annotation, usePolicies) in member
                    .getTaggedAnnotations<UsePolicy<*>>()) {
                usePolicies.single().policy?.also {
                    val rule = it.match(dispatch) ?:
                        throw PolicyRejectedException(it, member,
                                "The policy for @${annotation.annotationClass.simpleName} rejected '$member'")
                    val binding    = rule.bind(it, dispatch, annotation)
                    val descriptor = _policies.getOrPut(it) {
                        CallbackPolicyDescriptor(it) }
                    member.isAccessible = true
                    descriptor.add(binding)
                }
            }
        }
    }

    private fun isInstanceMethod(member: KCallable<*>) : Boolean
    {
        val parameters = member.parameters
        return parameters.isNotEmpty() &&
                parameters[0].kind == KParameter.Kind.INSTANCE
    }

    companion object {
        inline fun <reified T> getDescriptorFor() = getDescriptorFor(T::class)

        fun getDescriptorFor(handlerClass: KClass<*>) : HandlerDescriptor {
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

        fun getHandlersClasses(policy: CallbackPolicy, callback: Any) =
                DESCRIPTORS.mapNotNull {
                    it.value.value._policies[policy]?.let {
                    it.getInvariantMethods(callback).firstOrNull()
                    ?: it.getCompatibleMethods(callback).firstOrNull() }
                }.sortedWith(policy.methodBindingComparator)
                 .map { it.dispatcher.owningClass }
                 .distinct()

        fun getPolicyMethods(policy: CallbackPolicy, key: Any) =
                DESCRIPTORS.values.flatMap {
                    it.value._policies[policy]?.let {
                        it.getInvariantMethods(key) +
                        it.getCompatibleMethods(key)
                    } ?: emptyList()
                }.sortedWith(policy.methodBindingComparator)

        fun getPolicyMethods(policy: CallbackPolicy) =
                DESCRIPTORS.values.flatMap {
                    it.value._policies[policy]?.getInvariantMethods()
                            ?: emptyList()
                }

        private fun validateHandlerClass(handlerClass: KClass<*>) {
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