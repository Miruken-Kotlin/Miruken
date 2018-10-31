package com.miruken.callback

import kotlin.reflect.KType
import kotlin.reflect.full.createType

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
        return super.handleCallback(wrapped, TYPE, greedy, composer)
    }

    companion object {
        val TYPE = CompositionScope::class.createType()
    }
}