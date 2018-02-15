package com.miruken.callback

class FilteringHandler(
        handler: Handling,
        val filter:    (Any, Handling, () -> HandleResult) -> HandleResult,
        val reentrant: Boolean = false
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        if (!reentrant && callback is Composition) {
            return super.handleCallback(callback, greedy, composer)
        }
        return filter(callback, composer,
                { super.handleCallback(callback, greedy, composer)})
    }
}