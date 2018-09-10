package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.MemberBinding
import com.miruken.callback.policy.PolicyMemberBinding
import com.miruken.concurrent.Promise
import com.miruken.runtime.isCompatibleWith
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.jvmErasure

open class DynamicFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
    override var order: Int? = null

    final override fun next(
            callback: Cb,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ) = NEXT.getOrPut(this::class) { lazyOf(getDynamicNext(binding)) }
            .value?.let { dispatcher ->
                @Suppress("UNCHECKED_CAST")
                resolveArguments(dispatcher, callback,
                        binding, composer, next, provider)
                        ?.let { args ->
                            dispatcher.invoke(this, args) as Promise<Res>
                        }
            } ?: next()

    private fun resolveArguments(
            dispatcher: CallableDispatch,
            callback:   Cb,
            binding:    MemberBinding,
            composer:   Handling,
            next:       Next<Res>,
            provider:   FilteringProvider?
    ): Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == 2) {
            return arrayOf(callback, next)
        }
        val parent   = callback as? Inquiry
        val resolved = arrayOfNulls<Any?>(arguments.size)
        return composer.all {
            loop@ for (i in 2 until resolved.size) {
                val argument      = arguments[i]
                val argumentClass = argument.typeInfo.logicalType.jvmErasure
                when {
                    argumentClass == Handling::class ->
                        resolved[i] = composer
                    argumentClass.isInstance(binding) ->
                        resolved[i] = binding
                    argumentClass.isInstance(provider) ->
                        resolved[i] = provider
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
                            resolved[i] = resolver.resolve(
                                    key, typeInfo, it, composer, parent)
                        }) { result ->
                            if (optional) HandleResult.HANDLED else result
                        }
                    }
                }
            }
        } success {
            resolved[0] = callback
            resolved[1] = next
            resolved
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
                }?.let { CallableDispatch(it) }
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