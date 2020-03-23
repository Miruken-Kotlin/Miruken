package com.miruken.callback.policy.bindings

import com.miruken.Either
import com.miruken.TypeFlags
import com.miruken.TypeReference
import com.miruken.callback.*
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.concurrent.all
import com.miruken.fold
import com.miruken.runtime.closeType
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
) : MemberBinding() {
    val rule        = bindingInfo.rule
    val annotation  = bindingInfo.annotation
    val dispatcher  = bindingInfo.dispatcher
    val strict      = bindingInfo.strict
    val callbackArg = bindingInfo.callbackArg
    val key         = policy.createKey(bindingInfo)

    override val returnType get() = dispatcher.returnType

    override val skipFilters =
        dispatcher.findAnnotation<SkipFilters>() != null ||
        dispatcher.owningClass.findAnnotation<SkipFilters>() != null

    fun approve(callback: Any) = policy.approve(callback, this)

    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: TypeReference?,
            composer:     Handling,
            descriptor:   HandlerDescriptor,
            results:      CollectResultsBlock
    ) = (callback as? DispatchingCallbackGuard)?.let {
        it.tryDispatch(handler, dispatcher) {
            dispatchCore(handler, callback, callbackType,
                    composer, descriptor, results)
        } ?: HandleResult.NOT_HANDLED
    } ?: dispatchCore(handler, callback, callbackType,
            composer, descriptor, results)

    private fun dispatchCore(
            handler:      Any,
            callback:     Any,
            callbackType: TypeReference?,
            composer:     Handling,
            descriptor:   HandlerDescriptor,
            results:      CollectResultsBlock
    ): HandleResult {
        val ruleArgs = rule?.resolveArguments(callback) ?: emptyArray()
        val filterCallback = callbackArg?.let {
            ruleArgs[it.parameter.index - 1].takeIf { // skip receiver
                arg -> it.parameterType.jvmErasure.isInstance(arg)
            } ?: return HandleResult.NOT_HANDLED
        } ?: callback

        val typeBindings = lazy(LazyThreadSafetyMode.NONE) {
            val bindings = mutableMapOf<KTypeParameter, KType>()
            if (callbackType != null) {
                callbackArg?.typeInfo?.mapOpenParameters(
                        callbackType.kotlinType, bindings)
            }
            policy.getResultType(callback)?.also {
                dispatcher.returnInfo.mapOpenParameters(it, bindings)
            }
            bindings
        }

        val canFilter = (callback as? FilteringCallback)?.canFilter != false

        val filters = if (canFilter) {
            val resultType = with (dispatcher.returnInfo) {
                if (flags has TypeFlags.OPEN) {
                    componentType.closeType(typeBindings.value)
                            ?: return HandleResult.NOT_HANDLED
                } else {
                    logicalType
                }
            }
            resolveFilters(handler, filterCallback,
                    callbackType, resultType, composer, descriptor)
                    ?: return HandleResult.NOT_HANDLED
        } else {
            emptyList()
        }

        var completed = true

        val result = if (filters.isEmpty()) {
            val args = resolveArguments(callback, ruleArgs, callbackType, composer, typeBindings)
            if (args == null) {
                completed = false
            } else {
                args.fold({ dispatcher.invoke(handler, it) },
                          { p -> p then { dispatcher.invoke(handler, it) }
                })
            }
        } else {
            filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (proceed) {
                    val args = resolveArguments(callback, ruleArgs, callbackType, comp, typeBindings)
                    if (args == null) {
                        completed = false
                        Promise.reject(NotHandledException(callback,
                                "${dispatcher.callable} is missing one or more dependencies"))
                    } else {
                        args.fold({ Promise.resolve(invoke(handler, it)) },
                                  { p -> p then { invoke(handler, it) } })
                    }
                } else {
                    completed = false
                    Promise.reject(NotHandledException(callback,
                            "${dispatcher.callable} was aborted"))
                }
            }, { pipeline, next -> { comp, proceed ->
                    if (proceed) {
                        pipeline.first.next(filterCallback, callback, this, comp,
                                { c, p -> next((c ?: comp), p ?: true)
                        }, pipeline.second)
                    } else {
                        completed = false
                        Promise.reject(NotHandledException(
                                "${dispatcher.callable} was aborted"))
                    }
                }
            })(composer, true)
        }

        if (!completed) {
            return HandleResult.NOT_HANDLED
        }

        val accepted = policy.acceptResult(result, this)
        if (accepted.handled && result != null && result !is HandleResult) {
            if (!results(result, strict)) {
                return if (accepted.stop)
                    HandleResult.NOT_HANDLED_AND_STOP
                else HandleResult.NOT_HANDLED
            }
        }
        return accepted
    }

    private fun invoke(handler: Any, args: Array<Any?>): Any? {
        val baseResult   = dispatcher.invoke(handler, args)
        val handleResult = when (baseResult) {
            is HandleResult -> baseResult
            else -> policy.acceptResult(baseResult, this)
        }
        if (!handleResult.handled) {
            throw NotHandledException(handleResult)
        }
        return baseResult
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveFilters(
            handler:      Any,
            callback:     Any,
            callbackType: TypeReference?,
            resultType:   KType,
            composer:     Handling,
            descriptor:   HandlerDescriptor
    ): List<Pair<Filtering<Any,Any?>, FilteringProvider>>? {
        val cbType = callbackType?.kotlinType
                ?: callbackArg?.parameterType
                ?: callback::class.starProjectedType
        val filterType = Filtering::class.createType(listOf(
                KTypeProjection.invariant(cbType),
                KTypeProjection.invariant(resultType)))
        return composer.getOrderedFilters(
                filterType, this, sequenceOf(
                filters, dispatcher.filterProviders,
                descriptor.filters, policy.filters,
                ((handler as? Filtering<*,*>)?.let {
                    listOf(FilterInstanceProvider(true, it))
                } ?: emptyList()))
        ) as? List<Pair<Filtering<Any,Any?>, FilteringProvider>>
    }

    private fun resolveArguments(
            callback:      Any,
            ruleArguments: Array<Any?>,
            callbackType:  TypeReference?,
            composer:      Handling,
            typeBindings:  Lazy<MutableMap<KTypeParameter, KType>>
    ): Either<Array<Any?>, Promise<Array<Any?>>>? {
        val arguments = dispatcher.arguments
        if (arguments.size == ruleArguments.size)
            return Either.Left(ruleArguments)

        val parent   = callback as? Inquiry
        val resolved = ruleArguments.copyOf(arguments.size)
        val promises = mutableListOf<Promise<*>>()

        for (i in ruleArguments.size until arguments.size) {
            val argument    = arguments[i]
            val typeInfo    = argument.typeInfo
            val logicalType = typeInfo.logicalType
            val optional    = typeInfo.flags has TypeFlags.OPTIONAL
            when (logicalType.classifier) {
                Handling::class -> resolved[i] = composer
                MemberBinding::class,
                PolicyMemberBinding::class -> resolved[i] = this
                KType::class -> {
                    if (callbackType == null) {
                        if (!optional) return null
                    } else {
                        resolved[i] = callbackType.kotlinType
                    }
                }
                TypeReference::class -> {
                    if (callbackType == null) {
                        if (!optional) return null
                    } else {
                        resolved[i] = callbackType
                    }
                }
                else -> {
                    if (argument.isOpen && typeBindings.value.isEmpty()) {
                        return null
                    }
                    val inquiry = argument.createInquiry(parent, typeBindings.value)?.apply {
                        if (!(typeInfo.flags has TypeFlags.LAZY || typeInfo.flags has TypeFlags.FUNC)) {
                            wantsAsync = true
                        }
                    } ?: return null
                    val resolver = KeyResolver.getResolver(argument.useResolver, composer)
                            ?: return null
                    resolver.validate(inquiry.key, typeInfo)
                    when (val arg = resolver.resolve(inquiry, typeInfo, composer)) {
                        is Promise<*> ->
                            if (typeInfo.flags has TypeFlags.PROMISE) {
                                resolved[i] = arg
                            } else {
                                when (arg.state) {
                                    PromiseState.FULFILLED -> resolved[i] = arg.get()
                                            ?: if (optional) null else return null
                                    PromiseState.PENDING -> promises.add(arg then {
                                        resolved[i] = it
                                    })
                                    else -> return null
                                }
                            }
                        else -> resolved[i] = arg
                    }
                }
            }
        }

        return when {
            promises.size == 1 ->
                Either.Right(promises[0] then { resolved })
            promises.size > 1 ->
                Either.Right(Promise.all(promises) then { resolved })
            else -> Either.Left(resolved)
        }
    }

    companion object OrderByArity : Comparator<PolicyMemberBinding> {
        override fun compare(o1: PolicyMemberBinding, o2: PolicyMemberBinding) =
                o2.dispatcher.arity - o1.dispatcher.arity
    }
}