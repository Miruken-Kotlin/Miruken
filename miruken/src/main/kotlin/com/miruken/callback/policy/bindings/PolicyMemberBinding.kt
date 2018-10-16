package com.miruken.callback.policy.bindings

import com.miruken.TypeFlags
import com.miruken.callback.*
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.runtime.closeType
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class PolicyMemberBinding(
        val policy: CallbackPolicy,
        bindingInfo: PolicyMemberBindingInfo
) : MemberBinding(bindingInfo.dispatcher.javaMember) {

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
            callbackType: KType?,
            composer:     Handling,
            descriptor:   HandlerDescriptor,
            results:      CollectResultsBlock?
    ): HandleResult {
        (callback as? DispatchingCallbackGuard)?.let {
            if (!it.canDispatch(handler, dispatcher))
                return HandleResult.NOT_HANDLED
        }

        val ruleArgs  = rule?.resolveArguments(callback) ?: emptyArray()
        val filterCallback = callbackArg?.let {
            ruleArgs[it.parameter.index - 1].takeIf { // skip receiver
                arg -> it.parameterType.jvmErasure.isInstance(arg)
            } ?: return HandleResult.NOT_HANDLED
        } ?: callback

        val typeBindings = lazy(LazyThreadSafetyMode.NONE) {
            val bindings = mutableMapOf<KTypeParameter, KType>()
            if (callbackType != null) {
                callbackArg?.typeInfo?.mapOpenParameters(
                        callbackType, bindings)
            }
            policy.getResultType(callback)?.also {
                dispatcher.returnInfo.mapOpenParameters(it, bindings)
            }
            bindings
        }
        val resultType = with (dispatcher.returnInfo) {
            if (flags has TypeFlags.OPEN) {
                componentType.closeType(typeBindings.value)
                        ?: return HandleResult.NOT_HANDLED
            } else {
                logicalType
            }
        }

        val filters = resolveFilters(handler, filterCallback,
                callbackType, resultType, composer, descriptor)
                ?: return HandleResult.NOT_HANDLED
        val result  = if (filters.isEmpty()) {
            val args = resolveArguments(callback, ruleArgs,
                    callbackType, composer, typeBindings)
                    ?: return HandleResult.NOT_HANDLED
            dispatcher.invoke(handler, args)
        } else try {
            filters.foldRight({ comp: Handling, proceed: Boolean ->
                if (!proceed) notHandled()
                val args = resolveArguments(callback,
                        ruleArgs, callbackType, comp,
                        typeBindings) ?: notHandled()
                val baseResult   = dispatcher.invoke(handler, args)
                val handleResult = when (baseResult) {
                    is HandleResult -> baseResult
                    else -> policy.acceptResult(baseResult, this)
                }
                if (!handleResult.handled) {
                    throw NotHandledException(handleResult)
                }
                Promise.resolve(baseResult)
            }, { pipeline, next -> { comp, proceed ->
                    if (!proceed) notHandled()
                    pipeline.first.next(filterCallback, this, comp, { c, p ->
                        next((c ?: comp).skipFilters(), p ?: true)
                    }, pipeline.second)
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
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            resultType:   KType,
            composer:     Handling,
            descriptor:   HandlerDescriptor
    ): List<Pair<Filtering<Any,Any?>, FilteringProvider>>? {
        if ((callback as? FilteringCallback)?.canFilter == false) {
            return emptyList()
        }
        val cbType = callbackType ?: callbackArg?.parameterType
                ?: callback::class.starProjectedType

        val filterType = Filtering::class.createType(listOf(
                KTypeProjection.invariant(cbType),
                KTypeProjection.invariant(resultType)))
        return composer.getOrderedFilters(
                filterType, this, sequenceOf(
                        dispatcher.filterProviders,
                        descriptor.filters,
                        policy.filters,
                        ((handler as? Filtering<*,*>)?.let {
                            listOf(FilterInstanceProvider(true, it))
                        } ?: emptyList()))
        ) as? List<Pair<Filtering<Any,Any?>, FilteringProvider>>
    }

    private fun resolveArguments(
            callback:      Any,
            ruleArguments: Array<Any?>,
            callbackType:  KType?,
            composer:      Handling,
            typeBindings:  Lazy<MutableMap<KTypeParameter, KType>>
    ): Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == ruleArguments.size)
            return ruleArguments

        val parent   = callback as? Inquiry
        val resolved = ruleArguments.copyOf(arguments.size)

        return composer.all {
            loop@ for (i in ruleArguments.size until arguments.size) {
                val argument      = arguments[i]
                val typeInfo      = argument.typeInfo
                val logicalType   = typeInfo.logicalType
                val argumentClass = logicalType.jvmErasure
                when (argumentClass) {
                    Handling::class -> resolved[i] = composer
                    MemberBinding::class,
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
                        if (argument.isOpen && typeBindings.value.isEmpty()) {
                            return@all HandleResult.NOT_HANDLED
                        }
                        val inquiry = argument.getInquiry(
                                parent, typeBindings.value)
                            ?: return@all HandleResult.NOT_HANDLED
                        val optional = typeInfo.flags has TypeFlags.OPTIONAL
                        val resolver = KeyResolver.getResolver(
                                argument.useResolver, composer)
                        if (resolver == null) {
                            if (optional) continue@loop
                            return@all HandleResult.NOT_HANDLED
                        }
                        resolver.validate(inquiry.key, typeInfo)
                        add({ resolved[i] = resolver.resolve(
                                inquiry, typeInfo, it, composer) }
                        ) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success { resolved }
    }

    companion object OrderByArity : Comparator<PolicyMemberBinding> {
        override fun compare(
                o1: PolicyMemberBinding,
                o2: PolicyMemberBinding
        ) =  o2.dispatcher.arity - o1.dispatcher.arity
    }
}