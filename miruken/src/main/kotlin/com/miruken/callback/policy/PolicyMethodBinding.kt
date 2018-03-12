package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.*
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class PolicyMethodBinding(
        val policy:  CallbackPolicy,
        bindingInfo: PolicyMethodBindingInfo
) : MethodBinding(bindingInfo.dispatcher.javaMethod) {

    val rule        = bindingInfo.rule
    val annotation  = bindingInfo.annotation
    val callbackArg = bindingInfo.callbackArg
    val dispatcher  = bindingInfo.dispatcher
    val strict      = bindingInfo.strict
    val key         = policy.createKey(bindingInfo)

    fun approve(callback: Any) = policy.approve(callback, this)

    fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  CollectResultsBlock?
    ): HandleResult {
        val ruleArgs  = rule.resolveArguments(callback)
        val arguments = resolveArguments(ruleArgs, composer)
        return arguments?.let {
            @Suppress("UNCHECKED_CAST")
            val filters = resolveFilters(handler, callback, composer)
                    as List<Filtering<Any,Any?>>
            val result  = if (filters.isEmpty())
                 dispatcher.invoke(handler, arguments)
            else filters.foldRight(
                    { dispatcher.invoke(handler, arguments) },
                    { pipeline, next -> {
                            pipeline.next(callback, this, composer, next)
                        }
                    })()

            val accepted = policy.acceptResult(result, this)
            if (accepted.handled && result != null &&
                    result !is HandleResult) {
                if (results?.invoke(result, strict) == false) {
                    return@let if (accepted.stop)
                        HandleResult.NOT_HANDLED_AND_STOP
                    else HandleResult.NOT_HANDLED
                }
            }
            accepted
        } ?: HandleResult.NOT_HANDLED
    }

    private fun resolveFilters(
            handler:  Any,
            callback: Any,
            composer: Handling
    ): List<Filtering<*,*>> {
        if ((callback as? FilteringCallback)?.canFilter == false) {
            return emptyList()
        }
        val callbackType = callbackArg?.parameterType
                ?: callback::class.starProjectedType
        val filterType   = Filtering::class.createType(listOf(
                KTypeProjection.invariant(callbackType),
                KTypeProjection.invariant(dispatcher.returnType)))
        return composer.getOrderedFilters(filterType, this,
                (handler as? Filtering<*,*>)?.let {
                    listOf(InstanceFilterProvider(it))
                } ?: emptyList(),
                dispatcher.useFilterProviders,
                dispatcher.useFilters)
    }

    private fun resolveArguments(
            ruleArguments: Array<Any?>,
            composer:      Handling
    ): Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == ruleArguments.size)
            return ruleArguments

        val resolved = ruleArguments.copyOf(arguments.size)

        return composer.all {
            loop@ for (i in ruleArguments.size until arguments.size) {
                val argument      = arguments[i]
                val argumentClass = argument.typeInfo.logicalType.jvmErasure
                when {
                    argumentClass == Handling::class ->
                        resolved[i] = composer
                    argumentClass.isInstance(this@PolicyMethodBinding) ->
                        resolved[i] = this@PolicyMethodBinding
                    else -> {
                        val key      = argument.key
                        val flags    = argument.typeInfo.flags
                        val optional = flags has TypeFlags.OPTIONAL
                        val resolver = getResolver(argument.useResolver, composer)
                        if (resolver == null) {
                            if (optional) continue@loop
                            return@all HandleResult.NOT_HANDLED
                        }
                        resolver.validate(key, flags)
                        add({
                            val arg = resolver.resolve(key, flags, it, composer)
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
            resolverClass: KClass<out KeyResolving>?,
            composer:      Handling
    ): KeyResolving? {
        return if (resolverClass == null)
            DefaultKeyResolver else resolverClass.objectInstance
                ?: composer.resolve(resolverClass) as? KeyResolving
    }

    companion object {
        object DefaultKeyResolver : KeyResolver()
    }
}