package com.miruken.callback

import kotlin.reflect.KType

class InferringHandler(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val resolving     = getInferCallback(callback, callbackType)
        val resolvingType = if (resolving === callback) callbackType else null
        return handler.handle(resolving, resolvingType, greedy, composer)
    }

    private fun getInferCallback(callback: Any, callbackType: KType?) =
            when (callback) {
                is InferringCallback -> callback.inferCallback()
                else -> Resolution.getResolving(callback, callbackType)
            }
}