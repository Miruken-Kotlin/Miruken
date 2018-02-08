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
inline infix fun <T, S> Promise<T>
        .seq(p: Promise<S>): Promise<S> = p

/**
 * map/fmap
 */
inline infix fun <T, S> Promise<T>
        .map(noinline f: (T) -> S): Promise<S> = then(f)

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
inline infix fun <T, S> Promise<T>
        .flatMap(noinline f: (T) -> Promise<S>): Promise<S> =
        then(f).unwrap()

infix fun <T> Promise<T>
        .flatMapError(f: (Throwable) -> Promise<T>): Promise<T> =
        catch(f).then {
            it.fold({ it }, { Promise.resolve(it) })
        }.unwrap()
