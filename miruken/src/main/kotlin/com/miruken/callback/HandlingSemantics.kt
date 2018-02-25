package com.miruken.callback

fun Handling.getSemantics(): CallbackSemantics? {
    val semantics = CallbackSemantics()
    return handle(semantics, true) success { semantics }
}

fun Handling.semantics(options: CallbackOptions) =
        CallbackSemanticsHandler(this, options)

val Handling.duck get() = semantics(CallbackOptions.DUCK)

val Handling.strict get() = semantics(CallbackOptions.STRICT)

val Handling.broadcast get() = semantics(CallbackOptions.BROADCAST)

val Handling.bestEffort get() = semantics(CallbackOptions.BEST_EFFORT)

val Handling.notify get() = semantics(CallbackOptions.NOTIFY)
