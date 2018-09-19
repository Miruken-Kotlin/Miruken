package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.mapOpenParameters
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
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

    override val skipFilters =
        dispatcher.findAnnotation<SkipFilters>() != null ||
        dispatcher.owningClass.findAnnotation<SkipFilters>() != null

    fun approve(callback: Any) = policy.approve(callback, this)

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            composer:     Handling,
            results:      CollectResultsBlock?
    ): HandleResult {
        (callback as? DispatchingCallbackGuard)?.let {
            if (!it.canDispatch(handler, this))
                return HandleResult.NOT_HANDLED
        }
        val ruleArgs  = rule?.resolveArguments(callback) ?: emptyArray()
        val filterCallback = callbackArg?.let {
            ruleArgs[it.parameter.index - 1].takeIf { // skip receiver
                arg -> it.parameterType.jvmErasure.isInstance(arg)
            } ?: return HandleResult.NOT_HANDLED
        } ?: callback
        val resultType = policy.getResultType(callback)
                ?: dispatcher.returnType
        val filters = resolveFilters(
                handler, filterCallback, resultType, composer)
                ?: return HandleResult.NOT_HANDLED
        val result  = if (filters.isEmpty()) {
            val args = resolveArguments(callback,
                    ruleArgs, callbackType, resultType, composer)
                    ?: return HandleResult.NOT_HANDLED
            dispatcher.invoke(handler, args)
        } else try {
            filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) notHandled()
                val args = resolveArguments(callback,
                        ruleArgs, callbackType, resultType, comp)
                        ?: notHandled()
                val baseResult   = dispatcher.invoke(handler, args)
                val handleResult = when (baseResult) {
                    is HandleResult -> baseResult
                    else -> policy.acceptResult(baseResult, this)
                }
                if (!handleResult.handled) {
                    throw NotHandledException(handleResult)
                }
                Promise.resolve(baseResult)
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
            handler:    Any,
            callback:   Any,
            resultType: KType,
            composer:   Handling
    ): List<Filtering<Any,Any?>>? {
        if ((callback as? FilteringCallback)?.canFilter == false) {
            return emptyList()
        }
        val callbackType = callbackArg?.parameterType
                ?: callback::class.starProjectedType
        val filterType   = Filtering::class.createType(listOf(
                KTypeProjection.invariant(callbackType),
                KTypeProjection.invariant(resultType)))
        return composer.getOrderedFilters(filterType, this,
                (handler as? Filtering<*,*>)?.let {
                    listOf(InstanceFilterProvider(it))
                } ?: emptyList(),
                dispatcher.useFilterProviders,
                dispatcher.useFilters
        ) as? List<Filtering<Any,Any?>>
    }

    private fun resolveArguments(
            callback:      Any,
            ruleArguments: Array<Any?>,
            callbackType:  KType?,
            resultType:    KType,
            composer:      Handling
    ): Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == ruleArguments.size)
            return ruleArguments

        val parent   = callback as? Inquiry
        val resolved = ruleArguments.copyOf(arguments.size)

        val typeBindings by lazy(LazyThreadSafetyMode.NONE) {
            val bindings = mutableMapOf<KTypeParameter, KType>()
            if (callbackType != null) {
                callbackArg?.typeInfo?.mapOpenParameters(
                        callbackType, bindings)
            }
            dispatcher.returnInfo.mapOpenParameters(resultType, bindings)
            bindings
        }

        return composer.all {
            loop@ for (i in ruleArguments.size until arguments.size) {
                val argument      = arguments[i]
                val typeInfo      = argument.typeInfo
                val logicalType   = typeInfo.logicalType
                val argumentClass = logicalType.jvmErasure
                when (argumentClass) {
                    Handling::class -> resolved[i] = composer
                    PolicyMemberBinding::class ->
                        resolved[i] = this@PolicyMemberBinding
                    KType::class -> {
                        if (callbackType == null) {
                            val flags = typeInfo.flags
                            if (!(flags has TypeFlags.OPTIONAL))
                                return@all HandleResult.NOT_HANDLED
                        } else {
                            resolved[i] = callbackType
                        }
                    }
                    else -> {
                        if (argument.isOpen && typeBindings.isEmpty()) {
                            return@all HandleResult.NOT_HANDLED
                        }
                        val key      = argument.getKey(typeBindings)
                        val optional = typeInfo.flags has TypeFlags.OPTIONAL
                        val resolver = KeyResolver.getResolver(
                                argument.useResolver, composer)
                        if (resolver == null) {
                            if (optional) continue@loop
                            return@all HandleResult.NOT_HANDLED
                        }
                        resolver.validate(key, typeInfo)
                        add({
                            resolved[i] = resolver.resolve(
                                    key, typeInfo, it, composer, parent)
                        }) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success { resolved }
    }

    companion object {
        val ORDER_BY_ARITY : Comparator<PolicyMemberBinding> =
                Comparator { a, b ->
                    b.dispatcher.arity - a.dispatcher.arity
                }
    }
}