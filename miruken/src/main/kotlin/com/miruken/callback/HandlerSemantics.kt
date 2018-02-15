package com.miruken.callback

fun Handling.getSemantics(): CallbackSemantics? {
    val semantics = CallbackSemantics()
    return handle(semantics, true) map semantics
}

fun Handling.semantics(options: CallbackOptions) =
        CallbackSemanticsHandler(this, options)

fun Handling.duck() = semantics(CallbackOptions.DUCK)

fun Handling.strict() = semantics(CallbackOptions.STRICT)

fun Handling.broadcast() = semantics(CallbackOptions.BROADCAST)

fun Handling.bestEffort() = semantics(CallbackOptions.BEST_EFFORT)

fun Handling.notify() = semantics(CallbackOptions.NOTIFY)
