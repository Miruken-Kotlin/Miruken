package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicyDispatching
import com.miruken.callback.policy.CollectResultsBlock
import kotlin.reflect.KType

class NoReceiverHandler : Handler(), CallbackPolicyDispatching {
    override fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock?
    ): HandleResult {
        return HandleResult.NOT_HANDLED
    }
}