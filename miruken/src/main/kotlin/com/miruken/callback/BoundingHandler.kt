package com.miruken.callback

import com.miruken.TypeReference

class BoundingHandler(
        handler: Handling,
        private val bounds: Any?
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val bounded = ((callback as? Trampoline)
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