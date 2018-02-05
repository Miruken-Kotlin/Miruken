package com.miruken

data class HandleResult(
        val handled: Boolean,
        val stop:    Boolean = false
) {
    inline fun otherwise(block: () -> HandleResult) =
        if (handled) this else block()
}

interface IHandler {
    fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler? = null
    ) : HandleResult
}