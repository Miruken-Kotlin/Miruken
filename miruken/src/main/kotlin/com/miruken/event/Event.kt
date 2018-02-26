package com.miruken.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

typealias EventHandler<E> = Consumer<E>

class Event<T> {
    private val _handlers = ConcurrentHashMap<Int, EventHandler<T>>()
    private val _index    = AtomicInteger(0)

    infix fun register(handler: EventHandler<T>): () -> Unit {
        val next = _index.incrementAndGet()
        _handlers[next] = handler
        return { _handlers.remove(next) }
    }

    infix fun register(handler: (T) -> Unit): () -> Unit =
            register(EventHandler { handler(it) })

    @JvmName("add")
    operator fun plusAssign(handler: EventHandler<T>) {
        _handlers[_index.incrementAndGet()] = handler
    }

    @JvmName("add")
    inline operator fun plusAssign(crossinline handler: (T) -> Unit) {
        this += EventHandler { handler(it) }
    }

    @JvmName("notify")
    operator fun invoke(event: T) {
        for ((_, handler) in _handlers)
            handler.accept(event)
    }

    fun clear() = _handlers.clear()
}