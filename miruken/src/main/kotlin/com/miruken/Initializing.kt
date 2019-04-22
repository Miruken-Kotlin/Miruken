package com.miruken

import com.miruken.concurrent.Promise

interface Initializing {
    var initialized: Boolean

    fun initialize(): Promise<*>?

    fun failedInitialize(t: Throwable? = null)
}