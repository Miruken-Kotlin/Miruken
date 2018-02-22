package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.runtime.getTaggedAnnotations
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf

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
            target:   Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ): HandleResult {
        return _policies[policy]?.let {
            (it.getInvariantMethods(callback) +
             it.getCompatibleMethods(callback))
                    .fold(HandleResult.NOT_HANDLED, { result, method ->
                        if ((result.handled && !greedy) || result.stop) {
                            return result
                        }
                        result or method.dispatch(
                                target, callback, composer, results)
                    })
        }?: HandleResult.NOT_HANDLED
    }

    private fun findCompatibleMembers() {
        handlerClass.members.filter(::isInstanceMethod).forEach { member ->
            val dispatch = lazy(LazyThreadSafetyMode.NONE,
                    { MethodDispatch(member)})
            for ((annotation, usePolicies) in member
                    .getTaggedAnnotations<UsePolicy<*>>()) {
                usePolicies.single().policy?.also {
                    val rule = it.match(dispatch.value) ?:
                        throw IllegalStateException(
                                "The policy for @${annotation.annotationClass.simpleName} rejected '$member'"
                        )

                    val binding    = rule.bind(dispatch.value, annotation)
                    val descriptor = _policies.getOrPut(it) {
                        CallbackPolicyDescriptor(it) }
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

        fun getHandlersClasses(policy: CallbackPolicy, callback: Any) =
                DESCRIPTORS.mapNotNull {
                    it.value.value._policies[policy]?.let {
                    it.getInvariantMethods(callback).firstOrNull()
                    ?: it.getCompatibleMethods(callback).firstOrNull() }
                }.sortedWith(Comparator { a, b -> policy.compare(a.key, b.key) })
                 .map { it.dispatch.owningClass }
                 .distinct()

        fun getPolicyMethods(policy: CallbackPolicy, key: Any) =
                DESCRIPTORS.values.flatMap {
                    it.value._policies[policy]?.let {
                        it.getInvariantMethods(key) +
                        it.getCompatibleMethods(key)
                    } ?: emptyList()
                }.sortedWith(Comparator { a, b -> policy.compare(a.key, b.key) })

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
            check(!handlerClass.isSubclassOf(Collection::class)) {
                "Handlers cannot be collection classes: ${handlerClass.qualifiedName}"
            }
        }

        private val DESCRIPTORS =
                ConcurrentHashMap<KClass<*>, Lazy<HandlerDescriptor>>()
    }
}