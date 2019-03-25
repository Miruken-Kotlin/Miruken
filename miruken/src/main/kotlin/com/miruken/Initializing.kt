package com.miruken

import com.miruken.concurrent.Promise

interface Initializing {
    fun initialize(): Promise<*>?
}