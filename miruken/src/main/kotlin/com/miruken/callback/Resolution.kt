package com.miruken.callback

open class Resolution(key: Any, val callback: Any)
    : Inquiry(key, true), Resolving {

    override fun getResolveCallback(): Any = this

    companion object {
        fun getDefaultResolvingCallback(callback: Any) : Any {
            TODO("not implemented")
        }
    }
}