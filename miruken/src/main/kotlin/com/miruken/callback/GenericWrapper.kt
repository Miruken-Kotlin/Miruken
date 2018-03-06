package com.miruken.callback

import com.miruken.TypedValue
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class GenericWrapper(
        override val value: Any,
        override val type:  KType
) : Handler(), TypedValue {
    private val _handler = value as? Handling
            ?: HandlerAdapter(this)

    @Provides
    fun provide(inquiry: Inquiry) : Any? =
            if (isAssignableTo(inquiry.key, type))
                value else null

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ) = super.handleCallback(callback, greedy, composer)
            .otherwise(greedy) {
                _handler.handle(callback, greedy, composer)
            }
}