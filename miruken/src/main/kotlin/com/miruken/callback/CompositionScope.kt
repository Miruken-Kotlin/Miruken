package com.miruken.callback

class CompositionScope(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult {
        val wrapped =
                if (callback::class === Composition::class)
                    callback else Composition(callback)
        return super.handleCallback(wrapped, greedy, composer)
    }
}