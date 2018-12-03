package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo
import com.miruken.callback.policy.rules.MethodRule
import kotlin.reflect.KType

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy(
        val rules:  List<MethodRule>,
        filters:    Collection<FilteringProvider>,
        val strict: Boolean = false
) : FilteredObject(filters), Comparator<Any> {

    fun match(method: CallableDispatch) =
            rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(bindingInfo: PolicyMemberBindingInfo) =
            PolicyMemberBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMemberBindingInfo) =
            bindingInfo.inKey ?: bindingInfo.outKey

    abstract fun getKey(callback: Any, callbackType: TypeReference?): Any?

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

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ): HandleResult {
        if (handler is CallbackPolicyDispatching) {
            return handler.dispatch(this, callback, callbackType,
                    greedy, composer, results)
        }
        return HandlerDescriptorFactory.current.getDescriptor(handler::class)
                ?.dispatch(this, handler, callback, callbackType,
                        greedy, composer, results)
                ?: HandleResult.NOT_HANDLED
    }

    protected fun compareGenericArity(o1: Any?, o2: Any?): Int {
        val type2 = TypeReference.getKType(o2) ?: return 0
        val type1 = TypeReference.getKType(o1) ?: return 0
        return type2.arguments.size - type1.arguments.size
    }

    val orderMembers : Comparator<PolicyMemberBinding> =
            Comparator { a, b ->
                when (val order = compare(a.key, b.key)) {
                    0 -> b.dispatcher.arity - a.dispatcher.arity
                    else -> order
                }
            }

    companion object {
        fun getCallbackPolicy(callback: Any) =
                (callback as? DispatchingCallback)?.policy
                        ?: HandlesPolicy
    }
}