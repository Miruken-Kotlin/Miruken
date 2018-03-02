package com.miruken.error

import com.miruken.callback.Handler
import com.miruken.callback.RejectedException
import com.miruken.concurrent.Promise

class ErrorsHandler : Handler(), Errors {
    override fun handleException(
            exception: Throwable,
            callback:  Any?,
            context:   Any?
    ): Promise<*> {
        println("Unhandled exception: $exception")
        return Promise.reject(RejectedException(callback))
    }
}