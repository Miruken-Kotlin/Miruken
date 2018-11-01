package com.miruken.callback

import kotlin.reflect.KType

class BoundingHandler(
        handler: Handling,
        private val bounds: Any?
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val bounded = ((callback as? Composition)
                ?.callback ?: callback) as? BoundingCallback
        if (bounded != null && bounded.bounds == bounds)
            return HandleResult.NOT_HANDLED
        return super.handleCallback(
                callback, callbackType, greedy, composer)
    }
}

val Handling.stop get() = BoundingHandler(this, null)

fun Handling.stop(bounds: Any?) : Handling =
        BoundingHandler(this, bounds)