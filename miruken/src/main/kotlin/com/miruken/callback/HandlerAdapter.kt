package com.miruken.callback

open class HandlerAdapter(val handler: Any) : Handler() {
    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
            dispatch(handler, callback, greedy, composer)
}