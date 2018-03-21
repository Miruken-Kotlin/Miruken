package com.miruken.callback

import kotlin.reflect.KType

class CompositionScope(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val wrapped = when (callback::class) {
            Composition::class -> callback
            else -> Composition(callback, callbackType)
        }
        return super.handleCallback(wrapped, callbackType, greedy, composer)
    }
}