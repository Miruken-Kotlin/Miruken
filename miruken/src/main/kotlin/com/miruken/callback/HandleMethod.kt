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
    override val resultType: KType? = method.genericReturnType.toKType()
    override val policy:     CallbackPolicy? get() = null
    var          exception:  Throwable? = null

    override fun getResolveCallback() = Resolution(protocol, this)

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        return if (isAcceptableTarget(handler)) {
            BINDINGS.getOrPut(method) { HandleMethodBinding(method) }
                    .dispatch(handler, this, composer)
        } else HandleResult.NOT_HANDLED
    }

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