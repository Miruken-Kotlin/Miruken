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
        val bounded = callback as? BoundingCallback
        return if (bounded != null && bounded.bounds == bounds) {
            HandleResult.NOT_HANDLED
        } else {
            super.handleCallback(callback, callbackType, greedy, composer)
        }
    }
}

val Handling.stop get() = BoundingHandler(this, null)

fun Handling.stop(bounds: Any?) : Handling =
        BoundingHandler(this, bounds)