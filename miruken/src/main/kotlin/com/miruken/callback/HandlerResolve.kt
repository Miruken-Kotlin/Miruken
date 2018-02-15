package com.miruken.callback

fun Handling.resolve() = ResolvingHandler(this)

fun Handling.resolveAll() = CallbackSemanticsHandler(
        ResolvingHandler(this), CallbackOptions.BROADCAST)

fun Handling.resolve(key: Any) {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    if (handle(inquiry).handled) {
        val result = inquiry.result
        //return if (inquiry.isAsync)
    }
    return Unit
}
