package com.miruken.callback

import com.miruken.TypeReference

open class HandlerAdapter(val handler: Any) : Handler() {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = dispatch(handler, callback, callbackType, greedy, composer)
}