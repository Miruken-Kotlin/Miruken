package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy

open class Trampoline(val callback: Any) : DispatchingCallback {

    override val policy: CallbackPolicy?
        get() = (callback as? DispatchingCallback)?.policy

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
            Handler.dispatch(handler, callback, greedy, composer)
}