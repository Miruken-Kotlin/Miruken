package com.miruken.callback

import com.miruken.TypeReference

typealias FilterBlock = (Any, TypeReference?, Handling, () -> HandleResult) -> HandleResult

class FilteringHandler(
        handler:               Handling,
        private val filter:    FilterBlock,
        private val reentrant: Boolean = false
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        if (!reentrant && callback is Composition) {
            return super.handleCallback(
                    callback, callbackType, greedy, composer)
        }
        return filter(callback, callbackType, composer) {
            super.handleCallback(callback, callbackType, greedy, composer)
        }
    }
}

fun Handling.filter(filter: FilterBlock): Handling =
        FilteringHandler(this, filter)

fun Handling.filter(reentrant: Boolean, filter: FilterBlock): Handling =
        FilteringHandler(this, filter, reentrant)