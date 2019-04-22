package com.miruken.concurrent

import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule

fun Promise.Companion.all(vararg input:Any) : Promise<List<Any?>> =
        all(input.asList())

fun Promise.Companion.all(input: Collection<Any?>) : Promise<List<Any?>> {
    if (input.isEmpty())
        return resolve(emptyList())

    val pending   = AtomicInteger(0)
    val promises  = input.map(::resolve)
    val fulfilled = arrayOfNulls<Any>(promises.size)

    return Promise { resolveChild, rejectChild ->
        promises.forEachIndexed { index, promise ->
            promise.then {
                fulfilled[index] = it
                @Suppress("UNCHECKED_CAST")
                if (pending.incrementAndGet() == promises.size)
                    resolveChild(fulfilled.toList())
            }.catch {
                rejectChild(it)
            }
        }
    }
}

/**
 * Rejects promise if first promise is a rejection
 * Never returns if no promises are supplied.
 * Use [Promise].any instead
 */
fun Promise.Companion.race(vararg promises: Promise<*>) : Promise<*> =
        race(promises.toList())

fun Promise.Companion.race(promises: Collection<Promise<*>>) : Promise<*> {
    return Promise<Any?> { resolve, reject ->
        for (promise in promises) {
            promise.then(resolve, reject)
        }
    }
}

fun Promise.Companion.delay(delayMs: Long) : Promise<*> {
    return Promise<Any?> { resolve, _, onCancel ->
        val timer = Timer().schedule(delayMs) {
            resolve(null)
        }
        onCancel { timer.cancel() }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Promise<T>.timeout(timeoutMs: Long) : Promise<T> {
    val delay = Promise.delay(timeoutMs)
    return Promise.race(this.apply { finally {
        delay.cancel()
    }}, delay then {
        throw TimeoutException()
    }) as Promise<T>
}

inline fun <reified T> Promise.Companion.`try`(
        block: () -> T) : Promise<T> {
    return try {
        resolve(block())
    } catch (e: Throwable) {
        reject(e)
    }
}
