package com.miruken.callback

import kotlin.reflect.KType

class NoBatching(callback: Any)
    : Trampoline(callback), BatchingCallback {
    override val canBatch = false
}

class NoBatchingHandler(handler: Handling): DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val inquiry = callback as? Inquiry
        return if (inquiry?.keyClass == Batch::class) {
            HandleResult.NOT_HANDLED
        } else {
            super.handleCallback(callback, callbackType, greedy, composer)
        }
    }
}