package com.miruken.callback

class CascadeHandler(
        private val handlerA: Handling,
        private val handlerB: Handling) : Handler() {

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val result = super.handleCallback(callback, greedy, composer)
        return if (greedy) {
            result then {
                handlerA.handle(callback, greedy, composer)
            } then {
                handlerB.handle(callback, greedy, composer)
            }
        } else {
            result otherwise {
                handlerA.handle(callback, greedy, composer)
            } otherwise {
                handlerB.handle(callback, greedy, composer)
            }
        }
    }
}