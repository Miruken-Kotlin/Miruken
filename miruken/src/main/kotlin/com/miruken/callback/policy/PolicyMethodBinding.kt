package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import com.miruken.callback.all
import kotlin.reflect.jvm.jvmErasure

class PolicyMethodBinding(
        val policy:  CallbackPolicy,
        bindingInfo: PolicyMethodBindingInfo
) : MethodBinding(bindingInfo.dispatcher) {

    val rule             = bindingInfo.rule
    val annotation       = bindingInfo.annotation
    val callbackArgument = bindingInfo.callbackArgument
    val key              = policy.createKey(bindingInfo)

    fun approves(callback: Any) =
            policy.approve(callback, annotation)

    override fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock?
    ): HandleResult {
        return invoke(handler, callback, composer, results)
    }

    private fun invoke(
            receiver: Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock?
    ) : HandleResult {
        val ruleArgs  = rule.resolveArguments(callback)
        val arguments = resolveArguments(ruleArgs, composer)
        return arguments?.let {
            val result   = dispatcher.invoke(receiver, arguments)
            val accepted = policy.acceptResult(result, this)
            if (accepted.handled && result != null &&
                    result !is HandleResult) {
                results?.invoke(result, false)
            }
            accepted
        } ?: HandleResult.NOT_HANDLED
    }

    private fun resolveArguments(
            ruleArguments: Array<Any?>,
            composer:      Handling
    ) : Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == ruleArguments.size)
            return ruleArguments

        val resolved = ruleArguments.copyOf(arguments.size)

        return composer.all {
            for (i in ruleArguments.size until arguments.size) {
                val argument      = arguments[i]
                val argumentClass = argument.logicalType.jvmErasure
                when {
                    argumentClass == Handling::class ->
                        resolved[i] = composer
                    argumentClass.isInstance(this@PolicyMethodBinding) ->
                        resolved[i] = this@PolicyMethodBinding
                    else -> {
                        val resolver = argument.resolver
                        val optional = argument.flags has TypeFlags.OPTIONAL
                        add({
                            val arg = resolver.resolve(argument, it, composer)
                            resolved[i] = arg
                            arg
                        }) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success { resolved }
    }
}