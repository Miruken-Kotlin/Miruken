package com.miruken.callback

class OptionsHandler<T: Options<T>>(
        handler:     Handling,
        val options: T
) : DecoratedHandler(handler) {

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        @Suppress("UNCHECKED_CAST")
        val result = Composition.get(
                callback, options::class)?.let {
            options.mergeInto(it as T)
            HandleResult.HANDLED
        } ?: HandleResult.NOT_HANDLED
        return if (greedy) result or
            handler.handle(callback, greedy, composer)
        else result otherwise {
            handler.handle(callback, greedy, composer)
        }
    }
}
