package com.miruken.callback

open class CompositeHandler(vararg handlers: Any)
    : Handler(), CompositeHandling {

    private val _handlers = mutableListOf<Handling>()

    init {
        addHandlers(*handlers)
    }

    val handlers get() = _handlers.toList()

    override fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val initial = super.handleCallback(callback, greedy, composer)
        return handlers.fold(initial, { result, handler ->
            if (result.stop || (result.handled && !greedy)) {
                return result
            }
            result or handler.handle(callback, greedy, composer)
        })
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
            if (handler is HandlerAdapter && handler.handler === target)
                return handler
        }
        return null
    }
}