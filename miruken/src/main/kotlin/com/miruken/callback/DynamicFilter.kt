package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.HandleMethodBinding
import com.miruken.callback.policy.MethodBinding
import com.miruken.callback.policy.PolicyMethodBinding
import com.miruken.runtime.isCompatibleWith
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.jvmErasure

open class DynamicFilter<in Cb: Any, Res: Any?> : Filtering<Cb, Res> {
    override var order: Int? = null

    final override fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<Res>
    ) = NEXT.getOrPut(this::class) { getDynamicNext(binding) }
            ?.let { dispatcher ->
                @Suppress("UNCHECKED_CAST")
                resolveArguments(
                        dispatcher, callback, binding, composer, next)
                        ?.let { args ->
                            dispatcher.invoke(this, args)
                        } as Res
            } ?: next()

    private fun resolveArguments(
            dispatcher: CallableDispatch,
            callback:   Cb,
            binding:    MethodBinding,
            composer:   Handling,
            next:       Next<Res>
    ): Array<Any?>? {
        val arguments = dispatcher.arguments
        if (arguments.size == 2) {
            return arrayOf(callback, next)
        }
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
        } success {
            resolved[0] = callback
            resolved[1] = next
            resolved
        }
    }

    private fun getDynamicNext(binding: MethodBinding): CallableDispatch? {
        val callbackType = (binding as? PolicyMethodBinding)
                ?.callbackArg?.parameterType
                ?: HandleMethodBinding.HANDLE_METHOD_TYPE
        return this::class.declaredMemberFunctions
                .firstOrNull {
                    it.name == "next" &&
                    it.parameters.size >= 3 &&
                    isCompatibleWith(it.parameters[1].type, callbackType) &&
                    isCompatibleWith(binding.returnType, it.returnType)
                }?.let { CallableDispatch(it) }
    }
}

private val NEXT = ConcurrentHashMap<KClass<*>, CallableDispatch?>()