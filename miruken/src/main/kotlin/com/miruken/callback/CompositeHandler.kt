package com.miruken.callback

class CompositeHandler(vararg handlers: Any)
    : Handler(), ICompositeHandler {

    private val _handlers = mutableListOf<IHandler>()

    init {
        addHandlers(handlers)
    }

    val handlers = _handlers.toList()

    override fun addHandlers(vararg handlers: Any): ICompositeHandler {
        _handlers.addAll(handlers.filter {
            find(it) == null }.map(::toHandler))
        return this
    }

    override fun insertHandlers(atIndex: Int, vararg handlers: Any): ICompositeHandler {
        _handlers.addAll(atIndex, handlers.filter {
            find(it) == null }.map(::toHandler))
        return this
    }

    override fun removeHandlers(vararg handlers: Any): ICompositeHandler {
        _handlers.removeAll(handlers.mapNotNull(::find))
        return this
    }

    private fun find(target: Any) : IHandler? {
        for (handler in _handlers) {
            if (handler === target) return handler
            if (handler is HandlerAdapter && handler.handler === target)
                return handler
        }
        return null
    }
}