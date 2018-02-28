package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.HandleMethodBinding
import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.isTopLevelInterfaceOf
import com.miruken.runtime.toKType
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
    override val resultType: KType? = method.returnType.toKType()
    override val policy:     CallbackPolicy? get() = null
    var exception: Throwable? = null

    override fun getResolveCallback() = Resolution(protocol, this)

    fun invokeOn(target: Any, composer: Handling): HandleResult {
        return if (isAcceptableTarget(target)) {
            BINDINGS.getOrPut(method) { HandleMethodBinding(method) }
                    .dispatch(target, this, composer)
        } else HandleResult.NOT_HANDLED
    }

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ) = invokeOn(handler, composer)

    private fun isAcceptableTarget(target: Any): Boolean {
        return when {
            semantics.hasOption(CallbackOptions.STRICT) ->
                    protocol.isTopLevelInterfaceOf(target::class)
            semantics.hasOption(CallbackOptions.DUCK) -> true
            else -> isAssignableTo(protocol, target)
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