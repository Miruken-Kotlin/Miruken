package com.miruken.callback

class BoundingHandler(
        handler: Handling,
        private val bounds: Any?
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val bounded = when (callback) {
            is Composition -> callback.callback
            else -> callback
        } as? BoundingCallback
        if (bounded == null || bounded.bounds != bounds)
            return super.handleCallback(callback, greedy, composer)
        return HandleResult.HANDLED
    }
}

fun Handling.stop(bounds: Any?) : Handling =
        BoundingHandler(this, bounds)