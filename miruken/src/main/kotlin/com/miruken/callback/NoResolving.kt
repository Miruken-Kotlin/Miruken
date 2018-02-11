package com.miruken.callback

class NoResolving(callback: Any)
    : Trampoline(callback), Resolving {

    override fun getResolveCallback(): Any = callback
}