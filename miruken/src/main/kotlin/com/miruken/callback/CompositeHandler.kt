package com.miruken.callback

import com.miruken.TypeReference

open class CompositeHandler(vararg handlers: Any)
    : Handler(), CompositeHandling {

    private val _handlers = mutableListOf<Handling>()

    init {
        addHandlers(*handlers)
    }

    val handlers get() = _handlers.toList()

    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = handlers.fold(
            super.handleCallback(
                    callback, callbackType, greedy, composer)
    ) { result, handler ->
            if (result.stop || (result.handled && !greedy)) {
                return@fold result
            } else {
                result or handler.handle(
                        callback, callbackType, greedy, composer)
            }
    }

    final override fun addHandlers(
            vararg handlers: Any): CompositeHandler {
        _handlers.addAll(handlers.filter {
            find(it) == null }.map { it.toHandler() })
        return this
    }

    final override fun insertHandlers(
            atIndex: Int, vararg handlers: Any): CompositeHandler {
        _handlers.addAll(atIndex, handlers.filter {
            find(it) == null }.map { it.toHandler() })
        return this
    }

    final override fun removeHandlers(
            vararg handlers: Any): CompositeHandler {
        _handlers.removeAll(handlers.mapNotNull(::find))
        return this
    }

    private fun find(target: Any): Handling? {
        for (handler in _handlers) {
            if (handler === target) return handler
            when (handler) {
                is HandlerAdapter ->
                    if (handler.handler == target)
                        return handler
                is GenericWrapper ->
                    if (handler.value == target)
                        return handler
            }
        }
        return null
    }
}