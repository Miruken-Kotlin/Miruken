package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.CallbackPolicyDispatching
import com.miruken.callback.policy.CollectResultsBlock
import com.miruken.callback.policy.HandlerDescriptorFactory
import kotlin.reflect.jvm.jvmErasure

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
                .fold(HandleResult.NOT_HANDLED) { result, type ->
            if ((result.handled && !greedy) || result.stop) {
                return result
            }
            result or (factory.getDescriptor(type.jvmErasure)?.dispatch(
                    policy, type, callback, callbackType, greedy, composer, results)
                    ?: HandleResult.NOT_HANDLED)
        }
    }
}