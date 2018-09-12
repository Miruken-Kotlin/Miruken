package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import kotlin.reflect.KType

interface CallbackPolicyDispatching {
    fun dispatch(
            handler:      Any,
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling,
            results:      CollectResultsBlock? = null
    ): HandleResult
}