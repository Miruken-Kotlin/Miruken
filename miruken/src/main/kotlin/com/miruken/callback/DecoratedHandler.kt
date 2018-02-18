package com.miruken.callback

open class DecoratedHandler(val handler: Handling) : Handler() {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
        handler.handle(callback, greedy, composer) otherwise {
            super.handleCallback(callback, greedy, composer)
        }
}