package com.miruken.concurrent

import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule

fun Promise.Companion.all(vararg results:Any) : Promise<List<Any>> =
        all(results.asList())

fun Promise.Companion.all(results: Collection<Any>) : Promise<List<Any>> {
    if (results.isEmpty())
        return Promise.resolve(emptyList())

    val pending   = AtomicInteger(0)
    val promises  = results.map(::resolve)
    val fulfilled = arrayOfNulls<Any>(promises.size)

    return Promise { resolveChild, rejectChild ->
        for (index in 1..promises.size) {
            promises[index].then({
                fulfilled[index] = it
                @Suppress("UNCHECKED_CAST")
                if (pending.incrementAndGet() == promises.size)
                    resolveChild(fulfilled.toList() as List<Any>)
            }, rejectChild)
        }
    }
}

fun Promise.Companion.race(vararg promises: Promise<Any>) : Promise<Any> =
        race(promises.toList())

fun Promise.Companion.race(promises: Collection<Promise<Any>>) : Promise<Any> {
    return Promise { resolve, reject ->
        for (promise in promises) {
            promise.then(resolve, reject)
        }
    }
}

fun Promise.Companion.delay(delayMs: Long) : Promise<Unit> {
    var timer: TimerTask? = null
    return Promise<Unit> { resolve, _ ->
        timer = Timer().schedule(delayMs) {
            resolve(Unit)
        }
    } finally {
        timer?.cancel()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> Promise<T>.timeout(timeoutMs: Long) : Promise<T> {
    return Promise.race(this, Promise.delay(timeoutMs).then {
        throw TimeoutException()
    }).then { it as T }
}

fun <T> Promise.Companion.run(block: () -> T) : Promise<T> {
    return Promise { resolve, reject ->
        try {
            resolve(block())
        } catch (e: Throwable) {
            reject(e)
        }
    }
}

fun <T> Promise.Companion.start(block: () -> Promise<T>) : Promise<T> {
    return Promise { resolve, reject ->
        try {
            block().then(resolve, reject)
        } catch (e: Throwable) {
            reject(e)
        }
    }
}
