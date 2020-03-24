package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch

open class Resolution(
        key:              Any,
        val callback:     Any,
        val callbackType: TypeReference?
) : Inquiry(key, true, callback as? Inquiry), InferringCallback {
    private var _handled = false

    final override fun inferCallback() = this

    override fun tryDispatch(
            target:     Any,
            dispatcher: CallableDispatch,
            block:      () -> HandleResult
    ) = super.tryDispatch(target, dispatcher) {
        (callback as? DispatchingCallbackGuard)
            ?.tryDispatch(target, dispatcher, block)
            ?: block()
        }

    override fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        if (_handled && !greedy) return true
        _handled = Handler.dispatch(
                resolution, callback, callbackType, greedy, composer
        ).handled || _handled
        return _handled
    }
}