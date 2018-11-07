package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling

interface CallbackPolicyDispatching {
    fun dispatch(
            policy:       CallbackPolicy,
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ): HandleResult
}