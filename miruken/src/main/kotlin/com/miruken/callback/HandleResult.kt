package com.miruken.callback

data class HandleResult(
        val handled: Boolean,
        val stop:    Boolean = false
) {
    inline fun then(block: () -> HandleResult) =
            if (stop) this else this + block()

    inline fun otherwise(block: () -> HandleResult) =
            if (handled || stop) this else block()

    operator fun plus(other: HandleResult): HandleResult =
            HandleResult(handled || other.handled, stop || other.stop)

}