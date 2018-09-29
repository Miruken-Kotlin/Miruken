package com.miruken.callback

import kotlin.reflect.KType

open class Handler : Handling {
    override fun handle(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling?
    ): HandleResult {
        val scope = composer
                ?: this as? CompositionScope
                ?: CompositionScope(this)
        return handleCallback(callback, callbackType, greedy, scope)
    }

    protected open fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = dispatch(this, callback, callbackType, greedy, composer)

    companion object {
        fun dispatch(
                handler:      Any,
                callback:     Any,
                callbackType: KType?,
                greedy:       Boolean,
                composer:     Handling
        ) = when {
            ExcludeTypes.contains(handler::class) ->
                HandleResult.NOT_HANDLED
            else -> ((callback as? DispatchingCallback) ?:
                    Command(callback, callbackType))
                    .dispatch(handler, callbackType, greedy, composer)
        }

        private val ExcludeTypes = setOf(Handler::class,
                CascadeHandler::class, CompositeHandler::class,
                CompositionScope::class)
    }
}