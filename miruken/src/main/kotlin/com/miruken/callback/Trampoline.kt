package com.miruken.callback

import com.miruken.callback.policy.CallableDispatch
import kotlin.reflect.KType

open class Trampoline(
        val callback: Any?,
        val callbackType: KType? = null
) : DispatchingCallback, DispatchingCallbackGuard {
    override val policy get() =
        (callback as? DispatchingCallback)?.policy

    override fun canDispatch(
            target:     Any,
            dispatcher: CallableDispatch
    ) = (callback as? DispatchingCallbackGuard)
            ?.canDispatch(target, dispatcher) != false

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = callback?.let {
        when (handler) {
            is Handling -> handler.handle(it, this.callbackType,
                    greedy, composer)
            else -> Handler.dispatch(handler, it, this.callbackType,
                    greedy, composer)
        }
    } ?: HandleResult.NOT_HANDLED
}