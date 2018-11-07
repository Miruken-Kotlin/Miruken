package com.miruken.callback

import com.miruken.Flags
import com.miruken.TypeReference

class CallbackSemanticsHandler(
        handler: Handling,
        options: Flags<CallbackOptions>
) : DecoratedHandler(handler) {

    private val semantics = CallbackSemantics(options)

    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        if (Composition.get<CallbackSemantics>(callback) != null)
            return HandleResult.NOT_HANDLED

        if (callback is CallbackSemantics) {
            semantics.mergeInto(callback)
            val result = HandleResult.HANDLED
            return if (greedy) result or
                handler.handle(callback, callbackType, greedy, composer)
             else result
        }

        if (callback is Composition)
            return handler.handle(callback, callbackType, greedy, composer)

        val greed = greedy ||
                semantics.hasOption(CallbackOptions.BROADCAST)

        if (semantics.hasOption(CallbackOptions.BEST_EFFORT))
        {
            return try {
                HandleResult.HANDLED or
                        handler.handle(callback, callbackType, greed, composer)
            } catch (e: RejectedException) {
                HandleResult.HANDLED
            }
        }

        return super.handleCallback(callback, callbackType, greed, composer)
    }
}