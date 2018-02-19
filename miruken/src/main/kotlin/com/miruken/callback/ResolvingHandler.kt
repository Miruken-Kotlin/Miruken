package com.miruken.callback

class ResolvingHandler(handler: Handling) : DecoratedHandler(handler) {

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val resolving = getResolvingCallback(callback)
        return handler.handle(resolving, greedy, composer)
    }

    private fun getResolvingCallback(callback: Any) : Any {
        return if (callback is ResolvingCallback)
            callback.getResolveCallback()
        else
            Resolution.getDefaultResolvingCallback(callback)
    }
}