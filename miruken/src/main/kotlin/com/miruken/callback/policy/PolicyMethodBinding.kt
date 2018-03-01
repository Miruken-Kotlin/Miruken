package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.runtime.normalize
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class PolicyMethodBinding(
        val policy:  CallbackPolicy,
        bindingInfo: PolicyMethodBindingInfo
) : MethodBinding(bindingInfo.dispatcher.javaMethod) {

    val rule             = bindingInfo.rule
    val annotation       = bindingInfo.annotation
    val callbackArgument = bindingInfo.callbackArgument
    val dispatcher       = bindingInfo.dispatcher
    val key              = policy.createKey(bindingInfo)

    private val _filters: MutableList<FilteringProvider> =
            dispatcher.annotations
                    .filterIsInstance<FilteringProvider>()
                    .normalize().toMutableList()

    val filters: Collection<FilteringProvider> = _filters

    inline val strict get() = dispatcher.strict

    fun approves(callback: Any) =
            policy.approve(callback, annotation)

    fun addFilters(vararg providers: FilteringProvider) =
            _filters.addAll(providers)

    fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock?
    ): HandleResult {
        val ruleArgs  = rule.resolveArguments(callback)
        val arguments = resolveArguments(ruleArgs, composer)
        return arguments?.let {
            val result   = dispatcher.invoke(handler, arguments)
            val accepted = policy.acceptResult(result, this)
            if (accepted.handled && result != null &&
                    result !is HandleResult) {
                results?.invoke(result, strict)
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
                        val resolver = getResolver(argument.useResolver, composer)
                        val optional = argument.flags has TypeFlags.OPTIONAL
                        add({
                            val arg = resolver.resolve(argument, it, composer)
                            resolved[i] = arg
                        }) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success { resolved }
    }

    private fun getResolver(
            resolverClass: KClass<out ArgumentResolving>?,
            composer:      Handling
    ) : ArgumentResolving {
        return resolverClass?.let {
            composer.resolve(it) as? ArgumentResolving
        } ?: DefaultResolver
    }

    companion object {
        object DefaultResolver : ArgumentResolver()
    }
}