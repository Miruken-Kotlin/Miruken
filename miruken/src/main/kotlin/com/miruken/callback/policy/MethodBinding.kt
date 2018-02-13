package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling

abstract class MethodBinding {
    abstract fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results:  ((Any, Boolean) -> Boolean)? = null
    ): HandleResult
}