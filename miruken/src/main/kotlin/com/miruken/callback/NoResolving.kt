package com.miruken.callback

import kotlin.reflect.KType

class NoResolving(callback: Any, callbackType: KType?)
    : Trampoline(callback, callbackType), ResolvingCallback {
    override fun getResolveCallback() = callback!!
}