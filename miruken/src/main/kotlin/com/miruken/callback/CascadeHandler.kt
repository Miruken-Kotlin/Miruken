package com.miruken.callback

class CascadeHandler(handlerA: Any, handlerB: Any) : Handler() {
    private val _handlerA: Handling = handlerA.toHandler()
    private val _handlerB: Handling = handlerB.toHandler()

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
            super.handleCallback(callback, greedy, composer)
                    .otherwise(greedy) {
                        _handlerA.handle(callback, greedy, composer)
                    }.otherwise(greedy) {
                        _handlerB.handle(callback, greedy, composer)
                    }
}