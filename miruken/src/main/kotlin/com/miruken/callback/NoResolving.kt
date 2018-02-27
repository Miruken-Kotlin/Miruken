package com.miruken.callback

class NoResolving(callback: Any)
    : Trampoline(callback), ResolvingCallback {
    override fun getResolveCallback(): Any = callback
}