package com.miruken.callback

enum class HandleResult(
        val handled: Boolean,
        val stop:    Boolean
) {
    HANDLED(true, false),
    HANDLED_AND_STOP(true, true),
    NOT_HANDLED(false, false),
    NOT_HANDLED_AND_STOP(false, true);

    inline infix fun then(
            block: HandleResult.() -> HandleResult) =
            if (stop) this else this or block()

    infix fun <T> success(block: HandleResult.() -> T) : T? {
        return if (handled) block() else null
    }

    infix fun <T> failure(block: HandleResult.() -> T) : T? {
        return if (!handled) block() else null
    }

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

    inline infix fun otherwise(
            block: HandleResult.() -> HandleResult) =
            if (handled || stop) this else block()

    infix fun or(other: HandleResult): HandleResult =
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

    infix fun and(other: HandleResult): HandleResult =
            when (handled && other.handled) {
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