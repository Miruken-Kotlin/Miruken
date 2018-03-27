package com.miruken.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

typealias EventHandler<E> = Consumer<E>

class Event<T> {
    private val _index    = AtomicInteger(0)
    private val _handlers = ConcurrentHashMap<Int, EventHandler<T>>()

    infix fun register(handler: EventHandler<T>): () -> Unit {
        val next = _index.incrementAndGet()
        _handlers[next] = handler
        return { _handlers.remove(next) }
    }

    infix fun register(handler: (T) -> Unit): () -> Unit =
            register(EventHandler { handler(it) })

    operator fun plusAssign(handler: EventHandler<T>) {
        _handlers[_index.incrementAndGet()] = handler
    }

    inline operator fun plusAssign(crossinline handler: (T) -> Unit) {
        this += EventHandler { handler(it) }
    }

    operator fun invoke(event: T) {
        for (handler in _handlers.values)
            handler.accept(event)
    }

    operator fun invoke(createEvent: () -> T) {
        if (_handlers.isNotEmpty()) {
            val event = createEvent()
            for (handler in _handlers.values)
                handler.accept(event)
        }
    }

    fun clear() = _handlers.clear()
}