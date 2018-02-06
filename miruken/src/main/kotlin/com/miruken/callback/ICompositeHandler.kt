package com.miruken.callback

interface ICompositeHandler {
    fun addHandlers(vararg handlers: Any) : ICompositeHandler

    fun insertHandlers(atIndex: Int, vararg handlers: Any) : ICompositeHandler

    fun removeHandlers(vararg handlers: Any) : ICompositeHandler
}