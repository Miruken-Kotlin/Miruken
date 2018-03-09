package com.miruken.callback

class NoBatching(callback: Any)
    : Trampoline(callback), BatchingCallback {
    override val canBatch = false
}

class NoBatchingHandler(handler: Handling): DecoratedHandler(handler) {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val inquiry = Composition.get(callback) ?: callback as? Inquiry
        if (inquiry?.keyClass == Batch::class) {
            return HandleResult.NOT_HANDLED
        }
        return super.handleCallback(callback, greedy, composer)
    }
}