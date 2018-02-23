package com.miruken.callback

class CascadeHandler(
        private val handlerA: Handling,
        private val handlerB: Handling) : Handler() {

    override fun handleCallback(
            callback: Any,
            greedy: Boolean,
            composer: Handling
    ): HandleResult =
            super.handleCallback(callback, greedy, composer)
                    .otherwise(greedy) {
                        handlerA.handle(callback, greedy, composer)
                    }.otherwise(greedy) {
                        handlerB.handle(callback, greedy, composer)
                    }
}