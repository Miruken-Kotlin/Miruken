package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch

open class Resolution(
        key:              Any,
        val callback:     Any,
        val callbackType: TypeReference?
) : Inquiry(key, true, callback as? Inquiry),
        InferringCallback, FilteringCallback,
        DispatchingCallbackGuard {
    private var _handled = false

    override fun inferCallback() = this

    override val canFilter = false

    override fun canDispatch(
            target:     Any,
            dispatcher: CallableDispatch
    ) = (callback as? DispatchingCallbackGuard)
            ?.canDispatch(target, dispatcher) != false

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