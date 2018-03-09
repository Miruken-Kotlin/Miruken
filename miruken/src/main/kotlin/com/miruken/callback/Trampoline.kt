package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy

open class Trampoline(val callback: Any?) : DispatchingCallback {

    override val policy: CallbackPolicy?
        get() = (callback as? DispatchingCallback)?.policy

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ) = callback?.let {
        Handler.dispatch(handler, it, greedy, composer)
    } ?: HandleResult.NOT_HANDLED
}