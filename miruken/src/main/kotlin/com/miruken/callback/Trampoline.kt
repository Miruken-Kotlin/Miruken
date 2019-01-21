package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch

open class Trampoline(
        val callback:     Any?,
        val callbackType: TypeReference? = null
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
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = callback?.let {
        Handler.dispatch(handler, it, this.callbackType, greedy, composer)
    } ?: Command(this, callbackType)
            .dispatch(handler, callbackType, greedy, composer)
}