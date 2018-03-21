package com.miruken.callback

import kotlin.reflect.KType

open class HandlerAdapter(val handler: Any) : Handler() {
    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult =
            dispatch(handler, callback, callbackType, greedy, composer)
}