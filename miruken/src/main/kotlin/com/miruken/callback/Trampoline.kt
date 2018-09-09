package com.miruken.callback

import com.miruken.callback.policy.PolicyMemberBinding
import kotlin.reflect.KType

open class Trampoline(
        val callback: Any?,
        val callbackType: KType? = null
) : DispatchingCallback, DispatchingCallbackGuard {
    override val policy get() =
        (callback as? DispatchingCallback)?.policy

    override fun canDispatch(
            handler: Any,
            binding: PolicyMemberBinding
    ) = (callback as? DispatchingCallbackGuard)
            ?.canDispatch(handler, binding) != false

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = callback?.let {
        Handler.dispatch(handler, it, this.callbackType, greedy, composer)
    } ?: HandleResult.NOT_HANDLED
}