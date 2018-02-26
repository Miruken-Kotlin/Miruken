package com.miruken.callback

@FunctionalInterface
interface Handling {
    fun handle(
            callback: Any,
            greedy:   Boolean = false,
            composer: Handling? = null
    ) : HandleResult
}