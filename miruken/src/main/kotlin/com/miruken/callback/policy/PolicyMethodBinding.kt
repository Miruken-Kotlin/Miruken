package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling

class PolicyMethodBinding(
        val policy: CallbackPolicy
) : MethodBinding() {

    override fun dispatch(
            handler:  Any,
            callback: Any,
            composer: Handling,
            results: ((Any, Boolean) -> Boolean)?
    ): HandleResult {
        TODO("not implemented")
    }
}