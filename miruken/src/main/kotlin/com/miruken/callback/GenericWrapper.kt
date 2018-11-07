package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.TypedValue
import com.miruken.runtime.isCompatibleWith

class GenericWrapper(
        override val value: Any,
        override val type: TypeReference
) : Handler(), TypedValue {
    private val _handler = value as? Handling
            ?: HandlerAdapter(value)

    @Provides
    fun provide(inquiry: Inquiry) =
            if (isCompatibleWith(inquiry.key, type))
                value else null

    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = super.handleCallback(callback, callbackType, greedy, composer)
            .otherwise(greedy) {
                _handler.handle(callback, callbackType, greedy, composer)
            }
}
