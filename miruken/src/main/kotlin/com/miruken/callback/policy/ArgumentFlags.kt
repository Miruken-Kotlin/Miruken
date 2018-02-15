package com.miruken.callback.policy

import com.miruken.Flags

object ArgumentFlags : Flags<ArgumentFlags>() {
    val NONE     = Flags<ArgumentFlags>(0)
    val LAZY     = Flags<ArgumentFlags>(1 shl 0)
    val LIST     = Flags<ArgumentFlags>(1 shl 1)
    val PROMISE  = Flags<ArgumentFlags>(1 shl 2)
    val OPTIONAL = Flags<ArgumentFlags>(1 shl 3)
    val OPEN     = Flags<ArgumentFlags>(1 shl 4)
}