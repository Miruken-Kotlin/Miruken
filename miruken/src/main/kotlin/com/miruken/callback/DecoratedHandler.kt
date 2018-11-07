package com.miruken.callback

import com.miruken.TypeReference

open class DecoratedHandler(val handler: Handling) : Handler() {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult =
        handler.handle(callback, callbackType, greedy, composer) otherwise {
            super.handleCallback(callback, callbackType, greedy, composer)
        }
}