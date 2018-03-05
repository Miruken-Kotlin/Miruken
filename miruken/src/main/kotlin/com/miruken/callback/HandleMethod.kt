package com.miruken.callback

import com.miruken.TypedValue
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.HandleMethodBinding
import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.isTopLevelInterfaceOf
import com.miruken.toKType
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

open class HandleMethod(
        val protocol:  KType,
        val method:    Method,
        val arguments: Array<Any?>,
        val semantics: CallbackSemantics = CallbackSemantics.NONE
) : Callback, ResolvingCallback, DispatchingCallback {

    override var result:     Any?   = null
    override val resultType: KType? = method.genericReturnType.toKType()
    override val policy:     CallbackPolicy? get() = null
    var          exception:  Throwable? = null

    override fun getResolveCallback() = Resolution(protocol, this)

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ) = getTarget(handler)?.let {
        BINDINGS.getOrPut(method) { HandleMethodBinding(method) }
                .dispatch(it, this, composer)
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
                    isAssignableTo(protocol, it.type)
                }?.value ?: target.takeIf {
                    isAssignableTo(protocol, target)
                }
            }
        }
    }

    companion object {
        fun requireComposer() {
            requireNotNull(HandleMethodBinding.COMPOSER.get()) {
                "Composer is not available.  Did you call this method directly?"
            }
        }

        private val BINDINGS =
            ConcurrentHashMap<Method, HandleMethodBinding>()
    }
}