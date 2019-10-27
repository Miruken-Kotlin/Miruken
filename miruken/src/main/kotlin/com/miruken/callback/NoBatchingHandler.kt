package com.miruken.callback

import com.miruken.TypeReference

class NoBatch(callback: Any) : Trampoline(callback),
        BatchingCallback, InferringCallback {
    override val canBatch = false

    override fun inferCallback() = this
}

class NoBatchingHandler(handler: Handling): DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val inquiry = ((callback as? Composition)?.callback
                ?: callback) as? Inquiry
        if (inquiry?.keyClass == Batch::class) {
            return HandleResult.NOT_HANDLED
        }
        return super.handleCallback(
                NoBatch(callback), callbackType, greedy, composer)
    }
}