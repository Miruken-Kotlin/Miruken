package com.miruken.callback

enum class HandleResult(
        val handled: Boolean,
        val stop:    Boolean
) {
    HANDLED(true, false),
    HANDLED_AND_STOP(true, true),
    NOT_HANDLED(false, false),
    NOT_HANDLED_AND_STOP(false, true);

    inline infix fun then(block: () -> HandleResult) =
            if (stop) this else this + block()

    infix fun otherwise(handled: Boolean): HandleResult =
            when (handled || this.handled) {
                true  -> when (stop) {
                    true  -> HANDLED_AND_STOP
                    false -> HANDLED
                }
                false -> when (stop) {
                    true  -> NOT_HANDLED_AND_STOP
                    false -> NOT_HANDLED
                }
            }

    inline infix fun otherwise(block: () -> HandleResult) =
            if (handled || stop) this else block()

    infix operator fun plus(other: HandleResult): HandleResult =
            when (handled || other.handled) {
                true  -> when (stop || other.stop) {
                    true  -> HANDLED_AND_STOP
                    false -> HANDLED
                }
                false -> when (stop || other.stop) {
                    true  -> NOT_HANDLED_AND_STOP
                    false -> NOT_HANDLED
                }
            }
}