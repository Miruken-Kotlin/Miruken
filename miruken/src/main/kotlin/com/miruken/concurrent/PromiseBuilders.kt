package com.miruken.concurrent

import com.miruken.Either
import com.miruken.fold
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule

fun Promise.Companion.all(results: Collection<Any>) : Promise<Array<out Any>> {
    if (results.isEmpty())
        return Promise.resolve(emptyArray())

    var pending   = AtomicInteger(0)
    val promises  = results.map(::resolve)
    var fulfilled = arrayOfNulls<Any>(promises.size)

    return Promise { resolveChild, rejectChild ->
        for (index in 1..promises.size) {
            promises[index].then({
                fulfilled[index] = it
                if (pending.incrementAndGet() == promises.size)
                    resolveChild(fulfilled as Array<Any>)
            }, rejectChild)
        }
    }
}

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

/**
 * seq
 */
infix fun <T, S> Promise<T>
        .seq(p: Promise<S>): Promise<S> = p

/**
 * map/fmap
 */
infix fun <T, S> Promise<T>
        .map(f: (T) -> S): Promise<S> = then(f)

infix fun <T> Promise<T>
        .mapError(f: (Throwable) -> T): Promise<T> =
        catch(f).then { it.fold({ it }, { it }) }

/**
 * apply/<*>/ap
 */
infix fun <T, S> Promise<(T) -> S>
        .apply(f: Promise<T>): Promise<S> =
        then { f.then(it) }.unwrap()

/**
 * flatMap/bind/chain/liftM
 */
infix fun <T, S> Promise<T>
        .flatMap(f: (T) -> Promise<S>): Promise<S> =
        then(f).unwrap()

fun <T, S, U> Promise<T>
        .flatMap(f: (T) -> Promise<S>,
                 t: (Throwable) -> U): Promise<Either<U, S>> =
        then(f, t).then {
            it.fold(
                    { Promise.resolve(Either.Left(it)) },
                    { it.then { Either.Right(it) } })
        }.unwrap()

infix fun <T> Promise<T>
        .flatMapError(f: (Throwable) -> Promise<T>): Promise<T> =
        catch(f).then {
            it.fold({ it }, { Promise.resolve(it) })
        }.unwrap()
