package com.miruken.mvc

import com.miruken.context.Context

class NavigationException : Exception {
    constructor(context: Context) {
        this.context = context
    }

    constructor(context: Context, cause: Throwable)
        : super(cause)
    {
        this.context = context
    }

    constructor(context: Context, message: String)
            : super(message)
    {
        this.context = context
    }

    var context: Context
        private set
}