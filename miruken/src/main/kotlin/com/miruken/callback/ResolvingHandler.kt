package com.miruken.callback

class ResolvingHandler(handler: Handling) : DecoratedHandler(handler) {

    private fun getResolvingCallback(callback: Any) : Any {
        return if (callback is ResolvingCallback)
            callback.getResolveCallback()
        else
            Resolution.getDefaultResolvingCallback(callback)
    }
}