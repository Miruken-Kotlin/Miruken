package com.miruken.callback

interface CompositeHandling {
    fun addHandlers(vararg handlers: Any) : CompositeHandling
    fun insertHandlers(atIndex: Int, vararg handlers: Any) : CompositeHandling
    fun removeHandlers(vararg handlers: Any) : CompositeHandling
}