package com.miruken.callback

open class DecoratedHandler(val decoratee: IHandler) : Handler() {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ): HandleResult =
        decoratee.handle(callback, greedy, composer).otherwise {
            super.handleCallback(callback, greedy, composer)
        }
}