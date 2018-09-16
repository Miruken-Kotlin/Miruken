package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CallbackPolicyDispatching
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptor
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

class NoReceiverHandler : Handler(), CallbackPolicyDispatching {
    override fun dispatch(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock?
    ) = HandlerDescriptor.getNoReceiverHandlers(
            policy, callback, callbackType).fold(HandleResult.NOT_HANDLED) {
        result, handler ->
            if ((result.handled && !greedy) || result.stop) {
                return result
            }
            val descriptor = HandlerDescriptor.getDescriptor(handler.jvmErasure)
            result or descriptor.dispatch(policy, handler, callback,
                    callbackType, greedy, composer, results)
        }
}