package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import com.miruken.callback.policy.rules.MethodRule
import kotlin.reflect.KClass
import kotlin.reflect.KType

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy(
        val rules:  List<MethodRule>,
        filters:    Collection<FilteringProvider>,
        val strict: Boolean = false
) : FilteredObject(), Comparator<Any> {

    init { addFilters(filters) }

    fun match(method: CallableDispatch) =
            rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(bindingInfo: PolicyMemberBindingInfo) =
            PolicyMemberBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMemberBindingInfo) =
            bindingInfo.inKey ?: bindingInfo.outKey

    abstract fun getKey(callback: Any, callbackType: KType?): Any?

    abstract fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any>

    open fun getResultType(callback: Any): KType? = null

    open fun acceptResult(result: Any?, binding: PolicyMemberBinding) =
            when (result) {
                null, Unit -> HandleResult.NOT_HANDLED
                is HandleResult -> result
                else -> HandleResult.HANDLED
            }

    open fun approve(callback: Any, binding: PolicyMemberBinding) = true

    fun getMethods() = HandlerDescriptor.getPolicyMembers(this)

    fun getMethods(key: Any) = HandlerDescriptor.getPolicyMembers(this, key)

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ): HandleResult {
        if (handler is CallbackPolicyDispatching) {
            return handler.dispatch(this, callback, callbackType,
                    greedy, composer, results)
        }
        return HandlerDescriptor.getDescriptor(handler::class)
                .dispatch(this, handler, callback, callbackType,
                        greedy, composer, results)
    }

    protected fun compareGenericArity(o1: Any?, o2: Any?) = when (o1) {
        is KType -> when (o2) {
            is KType -> o2.arguments.size - o1.arguments.size
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

    val orderMembers : Comparator<PolicyMemberBinding> =
            Comparator { a, b ->
                val order = compare(a.key, b.key)
                when (order) {
                    0 -> b.dispatcher.arity - a.dispatcher.arity
                    else -> order
                }
            }

    companion object {
        fun getCallbackPolicy(callback: Any) =
                (callback as? DispatchingCallback)?.policy
                        ?: HandlesPolicy

        fun getInstanceHandlers(
                callback:     Any,
                callbackType: KType? = null
        ): List<KType> {
            val policy = getCallbackPolicy(callback)
            return HandlerDescriptor.getInstanceHandlers(
                    policy, callback, callbackType)
        }

        fun getTypeHandlers(
                callback:     Any,
                callbackType: KType? = null
        ): List<KType> {
            val policy = getCallbackPolicy(callback)
            return HandlerDescriptor.getTypeHandlers(
                    policy, callback, callbackType)
        }

        fun getCallbackHandlers(
                callback:     Any,
                callbackType: KType? = null
        ): List<KType> {
            val policy = getCallbackPolicy(callback)
            return HandlerDescriptor.getCallbackHandlers(
                    policy, callback, callbackType)
        }

        fun getCallbackMethods(
                callback:     Any,
                callbackType: KType? = null
        ): List<PolicyMemberBinding> {
            val policy = getCallbackPolicy(callback)
            return policy.getKey(callback, callbackType)?.let {
                policy.getMethods(it)
            } ?: emptyList()
        }
    }
}