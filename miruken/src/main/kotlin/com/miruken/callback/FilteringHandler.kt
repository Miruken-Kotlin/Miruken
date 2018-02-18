package com.miruken.callback

typealias FilterBlock = (Any, Handling, () -> HandleResult) -> HandleResult

class FilteringHandler(
        handler:               Handling,
        private val filter:    FilterBlock,
        private val reentrant: Boolean = false
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

fun Handling.filter(filter: FilterBlock): Handling =
        FilteringHandler(this, filter)

fun Handling.filter(reentrant: Boolean, filter: FilterBlock): Handling =
        FilteringHandler(this, filter, reentrant)