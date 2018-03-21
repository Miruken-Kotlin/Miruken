package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import kotlin.reflect.KType

open class Trampoline(
        val callback: Any?,
        val callbackType: KType? = null
) : DispatchingCallback {

    override val policy: CallbackPolicy?
        get() = (callback as? DispatchingCallback)?.policy

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = callback?.let {
        Handler.dispatch(handler, it, this.callbackType, greedy, composer)
    } ?: HandleResult.NOT_HANDLED
}