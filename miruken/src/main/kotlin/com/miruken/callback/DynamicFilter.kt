package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.bindings.PolicyMemberBinding
import com.miruken.concurrent.Promise
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
            } ?: next.abort()

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

        for (i in 2 until resolved.size) {
            val argument      = arguments[i]
            val argumentClass = argument.typeInfo.logicalType.jvmErasure
            when {
                argumentClass == Handling::class ->
                    resolved[i] = composer
                argumentClass.isSubclassOf(MemberBinding::class) ->
                    resolved[i] = binding
                argumentClass.isSubclassOf(FilteringProvider::class)->
                    resolved[i] = provider
                else -> {
                    val inquiry = argument.getInquiry(parent)
                            ?: return null
                    val typeInfo = argument.typeInfo
                    val resolver = KeyResolver.getResolver(
                            argument.useResolver, composer) ?: return null
                    resolver.validate(inquiry, typeInfo)
                    resolved[i] = resolver.resolve(
                            inquiry, typeInfo, composer) ?:
                            if (typeInfo.flags has TypeFlags.OPTIONAL)
                                null else return null
                }
            }
        }

        resolved[0] = callback
        resolved[1] = next
        return resolved
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