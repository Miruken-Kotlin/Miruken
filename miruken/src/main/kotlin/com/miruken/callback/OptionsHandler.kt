package com.miruken.callback

class OptionsHandler<T: Options<T>>(
        handler: Handler,
        val options: T
) : DecoratedHandler(handler) {

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        return Composition.extract<T>(callback)?.let {
            options.mergeInto(it)
            HandleResult.HANDLED.takeIf { greedy }?.then {
                decoratee.handle(callback, greedy, composer) }
        } ?: decoratee.handle(callback, greedy, composer)
    }
}