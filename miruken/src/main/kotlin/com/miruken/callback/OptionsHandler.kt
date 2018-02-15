package com.miruken.callback

class OptionsHandler<T: Options<T>>(
        handler: Handling,
        val options: T
) : DecoratedHandler(handler) {

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val result = Composition.map(callback) { other: T ->
            options.mergeInto(other)
            HandleResult.HANDLED
        } ?: HandleResult.NOT_HANDLED
        return if (greedy) result then {
            decoratee.handle(callback, greedy, composer)
        } else result otherwise {
            decoratee.handle(callback, greedy, composer)
        }
    }
}