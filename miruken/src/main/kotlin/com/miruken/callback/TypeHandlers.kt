package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CallbackPolicyDispatching
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptor
import kotlin.reflect.jvm.jvmErasure

object TypeHandlers : Handler(), CallbackPolicyDispatching {
    override fun dispatch(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock?
    ) = HandlerDescriptor.getTypeHandlers(
            policy, callback, callbackType)
            .fold(HandleResult.NOT_HANDLED) { result, type ->
                if ((result.handled && !greedy) || result.stop) {
                    return result
                }
                val descriptor = HandlerDescriptor
                        .getDescriptor(type.jvmErasure)
                result or descriptor.dispatch(policy, type, callback,
                        callbackType, greedy, composer, results)
        }
}