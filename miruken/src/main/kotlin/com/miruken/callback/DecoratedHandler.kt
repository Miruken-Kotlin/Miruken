package com.miruken.callback

open class DecoratedHandler(val decoratee: Handling) : Handler() {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
        decoratee.handle(callback, greedy, composer).otherwise {
            super.handleCallback(callback, greedy, composer)
        }
}