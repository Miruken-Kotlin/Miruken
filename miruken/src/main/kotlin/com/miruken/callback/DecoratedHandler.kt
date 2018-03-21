package com.miruken.callback

import kotlin.reflect.KType

open class DecoratedHandler(val handler: Handling) : Handler() {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult =
        handler.handle(callback, callbackType, greedy, composer) otherwise {
            super.handleCallback(callback, callbackType, greedy, composer)
        }
}