package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy

open class Resolution(key: Any, val callback: Any)
    : Inquiry(key, true), ResolvingCallback {

    private var _handled = false

    override fun getResolveCallback(): Any = this

    override fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        if (_handled && !greedy) return true
        _handled = Handler.dispatch(
                resolution, callback, greedy, composer
        ).handled || _handled
        return _handled
    }

    companion object {
        fun getDefaultResolvingCallback(callback: Any) : Any {
            val handlers = CallbackPolicy.getCallbackHandlerClasses(callback)
            if (handlers.isEmpty()) return callback
            val bundle = Bundle(false)
                    .add({ it.handle(NoResolving(callback)) }) { it }
            handlers.forEach { handler ->
                bundle.add {
                    it.handle(Resolution(handler, callback))
                }
            }
            return bundle
        }
    }
}