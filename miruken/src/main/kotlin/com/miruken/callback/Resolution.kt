package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import kotlin.reflect.KType

open class Resolution(
        key:              Any,
        val callback:     Any,
        val callbackType: KType?
) : Inquiry(key, true), ResolvingCallback {

    private var _handled = false

    override fun getResolveCallback() = this

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

    companion object {
        fun getResolvingCallback(
                callback:     Any,
                callbackType: KType?
        ): Any {
            val handlers = CallbackPolicy
                    .getCallbackHandlerClasses(callback, callbackType)
            if (handlers.isEmpty()) return callback
            val bundle = Bundle(false).apply {
                add({ it.handle(NoResolving(callback, callbackType)) }) { it }
            }
            handlers.forEach { handler ->
                bundle add { it.handle(Resolution(
                        handler, callback, callbackType)) }
            }
            return bundle
        }
    }
}