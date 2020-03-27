package com.miruken.callback

import com.miruken.Either
import com.miruken.TypeFlags
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.concurrent.all
import com.miruken.fold
import com.miruken.runtime.isCompatibleWith
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

open class DynamicFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
    override var order: Int? = null

    final override fun next(
            callback:    Cb,
            rawCallback: Any,
            binding:     MemberBinding,
            composer:    Handling,
            next:        Next<Res>,
            provider:    FilteringProvider?
    ) = NEXT.getOrPut(this::class) { lazyOf(getDynamicNext(binding)) }
            .value?.let { dispatcher ->
                @Suppress("UNCHECKED_CAST")
                resolveArguments(dispatcher, callback, rawCallback,
                        binding, composer, next, provider)
                    ?.fold({ dispatcher.invoke(this, it, composer) },
                           { p -> p then { dispatcher.invoke(this, it, composer) } }
                    ) as? Promise<Res>
            } ?: next.abort()

    private fun resolveArguments(
            dispatcher:  CallableDispatch,
            callback:    Cb,
            rawCallback: Any,
            binding:     MemberBinding,
            composer:    Handling,
            next:        Next<Res>,
            provider:    FilteringProvider?
    ): Either<Array<Any?>, Promise<Array<Any?>>>?{
        val arguments = dispatcher.arguments
        if (arguments.size == 2) {
            return Either.Left(arrayOf<Any?>(callback, next))
        }
        val parent   = callback as? Inquiry
        val resolved = arrayOfNulls<Any?>(arguments.size)
        val promises = mutableListOf<Promise<*>>()
        resolved[0] = callback

        for (i in 1 until resolved.size) {
            val argument = arguments[i]
            if (i == 1) {
                if (argument.parameterType.classifier == Any::class) {
                    resolved[1] = rawCallback
                } else {
                    resolved[1] = next
                }
                continue
            } else if (i == 2 && resolved[1] === rawCallback) {
                resolved[2] = next
                continue
            }
            val typeInfo      = argument.typeInfo
            val argumentClass = typeInfo.logicalType.jvmErasure
            when {
                argumentClass == Handling::class ->
                    resolved[i] = composer
                argumentClass.isSubclassOf(MemberBinding::class) ->
                    resolved[i] = binding
                argumentClass.isSubclassOf(FilteringProvider::class)->
                    resolved[i] = provider
                else -> {
                    val inquiry = argument.createInquiry(parent)?.apply {
                        if (!(typeInfo.flags has TypeFlags.LAZY || typeInfo.flags has TypeFlags.FUNC)) {
                            wantsAsync = true
                        }
                    } ?: return null
                    val resolver = KeyResolver.getResolver(argument.useResolver, composer)
                            ?: return null
                    resolver.validate(inquiry, typeInfo)
                    when (val arg = resolver.resolve(inquiry, typeInfo, composer)) {
                        is Promise<*> ->
                            if (typeInfo.flags has TypeFlags.PROMISE) {
                                resolved[i] = arg
                            } else {
                                val optional = typeInfo.flags has TypeFlags.OPTIONAL
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

    private fun getDynamicNext(binding: MemberBinding): CallableDispatch? {
        val callbackType = (binding as? PolicyMemberBinding)
                ?.callbackArg?.parameterType
                ?: HandleMethod.TYPE
        return this::class.declaredMemberFunctions
                .firstOrNull {
                    it.name == "next" &&
                    it.parameters.size >= 3 &&
                    it.returnType.classifier == Promise::class &&
                    isCompatibleWith(it.parameters[1].type, callbackType) &&
                    isCompatibleReturn(binding.returnType, it.returnType)
                }?.let(::CallableDispatch)
    }

    private fun isCompatibleReturn(
            bindingReturnType: KType,
            filterReturnType:  KType
    ) = if (bindingReturnType.classifier == Promise::class) {
        isCompatibleWith(bindingReturnType, filterReturnType)
    } else {
        isCompatibleWith(bindingReturnType, filterReturnType.arguments[0].type!!)
    }
}

private val NEXT = ConcurrentHashMap<KClass<*>, Lazy<CallableDispatch?>>()