package com.miruken.callback

data class HandleResult(
        val handled: Boolean,
        val stop:    Boolean = false
) {
    inline infix fun then(block: () -> HandleResult) =
            if (stop) this else this + block()

    inline infix fun otherwise(handled: Boolean): HandleResult =
            if (this.handled) this else
                HandleResult(handled, stop)

    inline infix fun otherwise(block: () -> HandleResult) =
            if (handled || stop) this else block()

    infix operator fun plus(other: HandleResult): HandleResult =
            if (this == other || (handled && stop)) this else
                HandleResult(handled || other.handled, stop || other.stop)
}