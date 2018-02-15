package com.miruken.callback

class CallbackSemanticsHandler(
        handler: Handling,
        options: CallbackOptions
) : DecoratedHandler(handler) {

    val semantics = CallbackSemantics(options)

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        if (Composition.has<CallbackSemantics>(callback))
            return HandleResult.NOT_HANDLED

        if (callback is CallbackSemantics) {
            callback.mergeInto(semantics)
            val result = HandleResult.HANDLED
            return if (greedy) result +
                decoratee.handle(callback, greedy, composer)
             else result
        }

        if (callback is Composition)
            return decoratee.handle(callback, greedy, composer)

        val greed = greedy ||
                semantics.hasOption(CallbackOptions.BROADCAST)

        if (semantics.hasOption(CallbackOptions.BEST_EFFORT))
        {
            return try {
                HandleResult.HANDLED +
                        decoratee.handle(callback, greed, composer)
            } catch (ex: RejectedException) {
                HandleResult.HANDLED
            }
        }

        return super.handleCallback(callback, greed, composer)
    }
}