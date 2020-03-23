package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch
import kotlin.reflect.KType

open class Trampoline(
        val callback:     Any?,
        val callbackType: TypeReference? = null
) : Callback, DispatchingCallback, DispatchingCallbackGuard {

    override val resultType: KType?
        get() = (callback as? Callback)?.resultType

    override var result: Any?
        get() = (callback as? Callback)?.result
        set(value) {
            (callback as? Callback)?.result = value
        }

    override val policy get() =
        (callback as? DispatchingCallback)?.policy

    override fun tryDispatch(
            target:     Any,
            dispatcher: CallableDispatch,
            block:      () -> HandleResult?
    ) = if (callback is DispatchingCallbackGuard) {
            callback.tryDispatch(target, dispatcher, block)
    } else {
        block()
    }

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