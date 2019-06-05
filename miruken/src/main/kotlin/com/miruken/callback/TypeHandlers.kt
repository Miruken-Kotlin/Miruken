package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CallbackPolicyDispatching
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptorFactory

object TypeHandlers : Handler(), CallbackPolicyDispatching {
    override fun dispatch(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock
    ): HandleResult {
        val factory = HandlerDescriptorFactory.current
        return factory.getTypeHandlers(policy, callback, callbackType)
                .fold(HandleResult.NOT_HANDLED) { result, descriptor ->
            if ((result.handled && !greedy) || result.stop) {
                return result
            }
            result or (descriptor.dispatch(policy, descriptor.handlerClass,
                    callback, callbackType, greedy, composer, results))
        }
    }
}