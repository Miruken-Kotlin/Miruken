package com.miruken.callback

import com.miruken.Flags

@Suppress("Classname")
sealed class CallbackOptions(value: Long) : Flags<CallbackOptions>(value) {
    object NONE        : CallbackOptions(0)
    object DUCK        : CallbackOptions(1 shl 0)
    object STRICT      : CallbackOptions(1 shl 1)
    object BROADCAST   : CallbackOptions(1 shl 2)
    object BEST_EFFORT : CallbackOptions(1 shl 3)
    object NOTIFY      : CallbackOptions(+(BROADCAST + BEST_EFFORT))
}