package com.miruken.callback

class CompositionScope(handler: IHandler) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ): HandleResult {
        val wrapped =
                if (callback::class === Composition::class)
                    callback else Composition(callback)
        return super.handleCallback(wrapped, greedy, composer)
    }
}