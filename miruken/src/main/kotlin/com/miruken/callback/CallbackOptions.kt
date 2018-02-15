package com.miruken.callback

import com.miruken.Flags

object CallbackOptions : Flags<CallbackOptions>() {
    val NONE        = Flags<CallbackOptions>(0)
    val DUCK        = Flags<CallbackOptions>(1 shl 0)
    val STRICT      = Flags<CallbackOptions>(1 shl 1)
    val BROADCAST   = Flags<CallbackOptions>(1 shl 2)
    val BEST_EFFORT = Flags<CallbackOptions>(1 shl 3)
    val NOTIFY      = BROADCAST + BEST_EFFORT
}