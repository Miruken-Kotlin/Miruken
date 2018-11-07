package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.TypedValue
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.bindings.HandleMethodBinding
import com.miruken.runtime.isCompatibleWith
import com.miruken.runtime.isTopLevelInterfaceOf
import com.miruken.runtime.matchMethod
import com.miruken.toKType
import com.miruken.typeOf
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType

class HandleMethod(
        val protocol:  TypeReference,
        val method:    Method,
        val arguments: Array<Any?>,
        val semantics: CallbackSemantics = CallbackSemantics.NONE
) : Callback, InferringCallback, DispatchingCallback {

    override var result:     Any?  = null
    override val resultType: KType = method.genericReturnType.toKType()
    override val policy:     CallbackPolicy? get() = null
    var          exception:  Throwable? = null

    override fun inferCallback() =
            Resolution(protocol, this, TYPE)

    override fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = getTarget(handler)?.let { target ->
        val targetClass = target::class
        BINDINGS.getOrPut(method to targetClass) {
            targetClass.matchMethod(method)?.let {
                HandleMethodBinding(method, it)
            }
        }?.dispatch(target, this, composer)
    } ?: HandleResult.NOT_HANDLED

    private fun getTarget(target: Any): Any? {
        val typedTarget = target as? TypedValue
        return when {
            semantics.hasOption(CallbackOptions.STRICT) -> {
                typedTarget?.takeIf {
                    protocol.kotlinType.isTopLevelInterfaceOf(
                            it.type.kotlinType)
                }?.value ?: target.takeIf {
                    protocol.kotlinType.isTopLevelInterfaceOf(target::class)
                }
            }
            semantics.hasOption(CallbackOptions.DUCK) -> {
                typedTarget?.value ?: target
            }
            else -> {
                typedTarget?.takeIf {
                    isCompatibleWith(protocol, it.type)
                }?.value ?: target.takeIf {
                    isCompatibleWith(protocol, target)
                }
            }
        }
    }

    companion object {
        val TYPE = typeOf<HandleMethod>()

        private val BINDINGS = ConcurrentHashMap<
                Pair<Method, KClass<*>>, HandleMethodBinding?>()
    }
}

