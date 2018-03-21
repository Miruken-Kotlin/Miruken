package com.miruken.callback

import kotlin.reflect.KType

class ResolvingHandler(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val resolving     = getResolvingCallback(callback, callbackType)
        val resolvingType = if (resolving === callback) callbackType else null
        return handler.handle(resolving, resolvingType, greedy, composer)
    }

    private fun getResolvingCallback(callback: Any, callbackType: KType?) =
            when (callback) {
                is ResolvingCallback -> callback.getResolveCallback()
                else -> Resolution.getResolvingCallback(callback, callbackType)
            }
}