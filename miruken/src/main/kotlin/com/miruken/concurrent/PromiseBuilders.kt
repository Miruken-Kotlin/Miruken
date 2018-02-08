package com.miruken.concurrent

import com.miruken.Either
import com.miruken.fold
import java.util.concurrent.atomic.AtomicInteger

/**
fun Promise.Companion.all(results: Collection<Any>) : Promise<Array<Any>> {
    if (results.isEmpty())
        return Promise.resolve(emptyArray())

    var pending   = AtomicInteger(0)
    val promises  = results.map(::resolve)
    var fulfilled = arrayOfNulls<Any>(promises.size)
}
*/

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
