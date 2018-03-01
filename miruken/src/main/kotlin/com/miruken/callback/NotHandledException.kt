package com.miruken.callback

class NotHandledException(val callback: Any, message: String)
    : RuntimeException(message) {
    constructor(callback: Any) : this(callback,
            "Callback ${callback::class.qualifiedName} not handled")
}