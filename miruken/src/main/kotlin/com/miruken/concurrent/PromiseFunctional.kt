package com.miruken.concurrent

import com.miruken.Either
import com.miruken.fold

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

fun <T, S> Promise<T>
        .map(f: (T) -> S,
             t: (Throwable) -> S): Promise<S> =
        then(f, t).then {
            it.fold({ it }, { it })
        }

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

infix fun <T: Any> Promise<T>
        .flatMapError(f: (Throwable) -> Promise<T>): Promise<T> =
        catch(f).then { it: Either<Promise<T>, T> ->
            it.fold({ it },
                    { r: T -> Promise { suc, _ -> suc(r) }})
        }.unwrap()
