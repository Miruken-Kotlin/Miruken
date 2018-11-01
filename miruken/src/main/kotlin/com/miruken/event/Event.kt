package com.miruken.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

typealias EventHandler<T>  = (Any, T) -> Unit
typealias EventConsumer<E> = Consumer<E>

class Event<T> {
    private val _index    = AtomicInteger(0)
    private val _handlers = ConcurrentHashMap<Int, EventHandler<T>>()

    infix fun handle(handler: EventHandler<T>): () -> Unit {
        val next = _index.incrementAndGet()
        _handlers[next] = handler
        return { _handlers.remove(next) }
    }

    infix fun consume(consumer: EventConsumer<T>) =
            handle { _, event -> consumer.accept(event) }

    infix fun register(handler: (T) -> Unit) =
            handle { _, event -> handler(event) }

    operator fun plusAssign(handler: (T) -> Unit) {
        _handlers[_index.incrementAndGet()] =
                { _, event -> handler(event) }
    }

    operator fun invoke(event: T) {
        for (handler in _handlers.values)
            handler(this, event)
    }

    operator fun invoke(createEvent: () -> T) {
        if (_handlers.isNotEmpty()) {
            val event = createEvent()
            for (handler in _handlers.values)
                handler(this, event)
        }
    }

    fun clear() = _handlers.clear()
}