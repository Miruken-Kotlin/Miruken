package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class PolicyMemberBinding(
        val policy:  CallbackPolicy,
        bindingInfo: PolicyMemberBindingInfo
) : MemberBinding(bindingInfo.dispatcher.javaMember) {

    val rule        = bindingInfo.rule
    val annotation  = bindingInfo.annotation
    val dispatcher  = bindingInfo.dispatcher
    val strict      = bindingInfo.strict
    val callbackArg = bindingInfo.callbackArg
    val key         = policy.createKey(bindingInfo)

    override val returnType: KType
        get() = dispatcher.returnType

    fun approve(callback: Any) = policy.approve(callback, this)

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            composer:     Handling,
            results:      CollectResultsBlock?
    ): HandleResult {
        val ruleArgs  = rule.resolveArguments(callback)
        val filterCallback = callbackArg?.let {
            ruleArgs[it.parameter.index - 1].takeIf { // skip receiver
                arg -> it.parameterType.jvmErasure.isInstance(arg)
            } ?: return HandleResult.NOT_HANDLED
        } ?: callback
        val filters = resolveFilters(handler, filterCallback, composer)
        val result  = if (filters.isEmpty()) {
            val args = resolveArguments(ruleArgs, callbackType, composer)
                    ?: return HandleResult.NOT_HANDLED
            dispatcher.invoke(handler, args)
        } else try {
            filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) notHandled()
                val args = resolveArguments(ruleArgs, callbackType, comp)
                        ?: notHandled()
                Promise.resolve(dispatcher.invoke(handler, args))
            }, { pipeline, next ->
                { comp, proceed ->
                    if (!proceed) notHandled()
                    pipeline.next(filterCallback, this, comp, { c, p ->
                        next((c ?: comp).skipFilters(), p ?: true)
                    })
                }
            })(composer, true)
        } catch (e: Throwable) {
            when (e) {
                is HandleResultException -> return e.result
                else -> throw e
            }
        }

        val accepted = policy.acceptResult(result, this)
        if (accepted.handled && result != null &&
                result !is HandleResult) {
            if (results?.invoke(result, strict) == false) {
                return if (accepted.stop)
                    HandleResult.NOT_HANDLED_AND_STOP
                else HandleResult.NOT_HANDLED
            }
        }
        return accepted
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveFilters(
            handler:  Any,
            callback: Any,
            composer: Handling
    ): List<Filtering<Any,Any?>> {
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
                dispatcher.useFilters
        ) as List<Filtering<Any,Any?>>
    }

    private fun resolveArguments(
            ruleArguments: Array<Any?>,
            callbackType:  KType?,
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
                    argumentClass.isInstance(this@PolicyMemberBinding) ->
                        resolved[i] = this@PolicyMemberBinding
                    argumentClass == KType::class -> {
                        if (callbackType == null) {
                            val flags = argument.typeInfo.flags
                            if (!(flags has TypeFlags.OPTIONAL))
                                return@all HandleResult.NOT_HANDLED
                        } else {
                            resolved[i] = callbackType
                        }
                    }
                    else -> {
                        val key      = argument.key
                        val typeInfo = argument.typeInfo
                        val optional = typeInfo.flags has TypeFlags.OPTIONAL
                        val resolver = KeyResolver.getResolver(
                                argument.useResolver, composer)
                        if (resolver == null) {
                            if (optional) continue@loop
                            return@all HandleResult.NOT_HANDLED
                        }
                        resolver.validate(key, typeInfo)
                        add({
                            resolved[i] = resolver.resolve(key, typeInfo, it, composer)
                        }) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success { resolved }
    }
}