package com.miruken.callback.policy

import com.miruken.callback.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy(
        val rules:   List<MethodRule>,
        val filters: List<FilteringProvider>,
        val strict:  Boolean = false
) : Comparator<Any> {
    val methodBindingComparator : Comparator<PolicyMethodBinding> =
            Comparator { a, b -> compare(a.key, b.key) }

    fun match(method: CallableDispatch) =
            rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(
            bindingInfo: PolicyMethodBindingInfo
    ): PolicyMethodBinding =
            PolicyMethodBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMethodBindingInfo): Any? =
            bindingInfo.inKey ?: bindingInfo.outKey

    abstract fun getKey(callback: Any, callbackType: KType?): Any?

    abstract fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any>

    open fun acceptResult(result: Any?, binding: PolicyMethodBinding) =
         when (result) {
             null, Unit -> HandleResult.NOT_HANDLED
             is HandleResult -> result
             else -> HandleResult.HANDLED
         }

    open fun approve(callback: Any, binding: PolicyMethodBinding) = true

    fun getMethods() = HandlerDescriptor.getPolicyMethods(this)

    fun getMethods(key: Any) = HandlerDescriptor.getPolicyMethods(this, key)

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ) = HandlerDescriptor.getDescriptorFor(handler::class)
            .dispatch(this, handler, callback, callbackType,
                    greedy, composer, results)

    protected fun compareGenericArity(o1: Any?, o2: Any?) = when (o1) {
        is KType -> when (o2) {
            is KType -> o2.arguments.size -  o1.arguments.size
            is KClass<*> -> o2.typeParameters.size - o1.arguments.size
            is Class<*> -> o2.typeParameters.size - o1.arguments.size
            else -> 0
        }
        is KClass<*> -> when (o2) {
            is KType -> o2.arguments.size - o1.typeParameters.size
            is KClass<*> -> o2.typeParameters.size - o1.typeParameters.size
            is Class<*> -> o2.typeParameters.size -o1.typeParameters.size
            else -> 0
        }
        is Class<*> -> when (o2) {
            is KType -> o2.arguments.size - o1.typeParameters.size
            is Class<*> -> o2.typeParameters.size - o1.typeParameters.size
            else -> 0
        }
        else -> 0
    }

    companion object {
        fun getCallbackHandlerClasses(
                callback:     Any,
                callbackType: KType? = null
        ): List<KClass<*>> {
            val policy = getCallbackPolicy(callback)
            return HandlerDescriptor.getHandlersClasses(
                    policy, callback, callbackType)
        }

        fun getCallbackMethods(
                callback:     Any,
                callbackType: KType? = null
        ): List<PolicyMethodBinding> {
            val policy = getCallbackPolicy(callback)
            return policy.getKey(callback, callbackType)?.let {
                policy.getMethods(it)
            } ?: emptyList()
        }

        fun getCallbackPolicy(callback: Any) =
                (callback as? DispatchingCallback)?.policy
                        ?: HandlesPolicy
    }
}