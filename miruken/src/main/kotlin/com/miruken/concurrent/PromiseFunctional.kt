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
        then(f, t).then { r ->
            r.fold({ it }, { it })
        }

infix fun <T> Promise<T>
        .mapError(f: (Throwable) -> T): Promise<T> =
        catch(f).then { r -> r.fold({ it }, { it }) }

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
                 t: (Throwable) -> Promise<U>): Promise<Either<U, S>> =
        then(f, t).then { r ->
            r.fold(
                    { it.then { r -> Either.Left(r) } },
                    { it.then { r -> Either.Right(r) } })
        }.unwrap()

infix fun <T> Promise<T>
        .flatMapError(f: (Throwable) -> Promise<T>): Promise<T> =
        catch(f).then { it: Either<Promise<T>, T> ->
            it.fold({ it },
                    { r: T -> Promise { suc, _ -> suc(r) }})
        }.unwrap()

fun <T, S> Promise<T>
        .fold(f: (T) -> Promise<S>,
              t: (Throwable) -> Promise<S>): Promise<S> =
        flatMap(f, t) then { r -> r.fold({ it }, { it }) }
