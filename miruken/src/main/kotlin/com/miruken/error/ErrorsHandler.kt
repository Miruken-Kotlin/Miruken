package com.miruken.error

import com.miruken.callback.Handler
import com.miruken.callback.Provides
import com.miruken.callback.RejectedException
import com.miruken.callback.Singleton
import com.miruken.concurrent.Promise

class ErrorsHandler
    @Provides @Singleton constructor() : Handler(), Errors {

    override fun handleException(
            exception: Throwable,
            callback:  Any?,
            context:   Any?
    ): Promise<*> {
        println("Unhandled exception: $exception")
        return Promise.reject(RejectedException(callback))
    }
}