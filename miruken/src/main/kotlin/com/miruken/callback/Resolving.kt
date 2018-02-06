package com.miruken.callback

open class Resolving(key: Any, callback: Any)
    : Inquiry(key, true), IResolveCallback {

    override fun getResolveCallback(): Any = this

    companion object {
        fun getDefaultResolvingCallback(callback: Any) : Any {
            TODO("not implemented")
        }
    }
}