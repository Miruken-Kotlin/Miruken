package com.miruken.callback

import com.miruken.TypedValue
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.HandleMethodBinding
import com.miruken.runtime.isCompatibleWith
import com.miruken.runtime.isTopLevelInterfaceOf
import com.miruken.runtime.matchMethod
import com.miruken.toKType
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

open class HandleMethod(
        val protocol:  KType,
        val method:    Method,
        val arguments: Array<Any?>,
        val semantics: CallbackSemantics = CallbackSemantics.NONE
) : Callback, InferringCallback, DispatchingCallback {

    override var result:     Any?   = null
    override val resultType: KType? = method.genericReturnType.toKType()
    override val policy:     CallbackPolicy? get() = null
    var          exception:  Throwable? = null

    override fun inferCallback() =
            Resolution(protocol, this, TYPE)

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = getTarget(handler)?.let {
        val targetClass = it::class
        targetClass.matchMethod(method)?.let {
            BINDINGS.getOrPut(method to targetClass) {
                    HandleMethodBinding(method, it)
            }}?.dispatch(it, this, composer)
    } ?: HandleResult.NOT_HANDLED

    private fun getTarget(target: Any): Any? {
        val typedTarget = target as? TypedValue
        return when {
            semantics.hasOption(CallbackOptions.STRICT) -> {
                typedTarget?.takeIf {
                    protocol.isTopLevelInterfaceOf(it.type)
                }?.value ?: target.takeIf {
                    protocol.isTopLevelInterfaceOf(target::class)
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
        val TYPE = HandleMethod::class.createType()

        private val BINDINGS = ConcurrentHashMap<
                Pair<Method, KClass<*>>, HandleMethodBinding?>()
    }
}

