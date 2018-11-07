package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.typeOf

class CompositionScope(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val wrapped = when (callback::class) {
            Composition::class -> callback
            else -> Composition(callback, callbackType)
        }
        return super.handleCallback(wrapped, TYPE, greedy, composer)
    }

    companion object {
        val TYPE = typeOf<CompositionScope>()
    }
}
