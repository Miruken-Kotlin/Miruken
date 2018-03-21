package com.miruken.callback

import kotlin.reflect.KType

class CascadeHandler(handlerA: Any, handlerB: Any) : Handler() {
    private val _handlerA = handlerA.toHandler()
    private val _handlerB = handlerB.toHandler()

    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = super.handleCallback(callback, callbackType, greedy, composer)
            .otherwise(greedy) {
                _handlerA.handle(callback, callbackType, greedy, composer)
            }.otherwise(greedy) {
                _handlerB.handle(callback, callbackType, greedy, composer)
            }
}