package com.miruken.error

import com.miruken.concurrent.Promise

interface Errors {
    fun handleException(
            exception: Throwable,
            callback:   Any? = null,
            context:    Any? = null
    ): Promise<*>
}