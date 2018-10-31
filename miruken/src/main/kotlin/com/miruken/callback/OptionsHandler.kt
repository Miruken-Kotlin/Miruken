package com.miruken.callback

import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf
import kotlin.reflect.KType

class OptionsHandler<T: Options<T>>(
        private val options:     T,
        private val optionsType: KType,
        handler:                 Handling
) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = (callback.takeIf {
            isCompatibleWith(optionsType, it) }?.let {
            @Suppress("UNCHECKED_CAST")
            options.mergeInto(it as T)
            HandleResult.HANDLED
        } ?: super.handleCallback(
            callback, callbackType, greedy, composer))
            .otherwise {
                handler.handle(callback, callbackType, greedy, composer)
            }
}

inline fun <reified T: Options<T>> Handling.withOptions(options: T) =
        OptionsHandler(options, typeOf<T>(), this)