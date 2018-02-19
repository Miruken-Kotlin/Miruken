package com.miruken.callback.policy

import com.miruken.Flags

sealed class ArgumentFlags(value: Long) : Flags<ArgumentFlags>(value) {
    object NONE       : ArgumentFlags(0)
    object LAZY       : ArgumentFlags(1 shl 0)
    object COLLECTION : ArgumentFlags(1 shl 1)
    object PROMISE    : ArgumentFlags(1 shl 2)
    object OPEN       : ArgumentFlags(1 shl 3)
}