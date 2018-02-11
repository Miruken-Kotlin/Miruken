package com.miruken.callback

enum class HandleResult(
        val handled: Boolean,
        val stop:    Boolean
) {
    Handled(true, false),
    HandledAndStop(true, true),
    NotHandled(false, false),
    NotHandledAndStop(false, true);

    inline infix fun then(block: () -> HandleResult) =
            if (stop) this else this + block()

    infix fun otherwise(handled: Boolean): HandleResult =
            when (handled || this.handled) {
                true  -> when (stop) {
                    true  -> HandledAndStop
                    false -> Handled
                }
                false -> when (stop) {
                    true  -> NotHandledAndStop
                    false -> NotHandled
                }
            }

    inline infix fun otherwise(block: () -> HandleResult) =
            if (handled || stop) this else block()

    infix operator fun plus(other: HandleResult): HandleResult =
            when (handled || other.handled) {
                true  -> when (stop || other.stop) {
                    true  -> HandledAndStop
                    false -> Handled
                }
                false -> when (stop || other.stop) {
                    true  -> NotHandledAndStop
                    false -> NotHandled
                }
            }
}