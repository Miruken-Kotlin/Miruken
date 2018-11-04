package com.miruken.api

@Suppress("UNCHECKED_CAST")
sealed class Try<out E: Throwable, out R> {
    data class Failure<out E: Throwable, R>(val error: E) : Try<E, R>()
    data class Success<out E: Throwable, out R>(val result: R) : Try<E, R>()

    companion object {
        fun <E : Throwable> error(t: E) = Failure<E, Nothing>(t)
        fun <R> result(result: R) = Success<Nothing, R>(result)
    }
}

/**
 * seq
 */
infix fun <E: Throwable, R, R2> Try<E, R>
        .seq(e: Try<E, R2>): Try<E, R2> = e

/**
 * map/fmap
 */
infix fun <E: Throwable, R, R2> Try<E, R>
        .map(f: (R) -> R2): Try<E, R2> = when (this) {
            is Try.Failure -> Try.Failure(error)
            is Try.Success -> Try.Success(f(result))
}

/**
 * apply/<*>/ap
 */
infix fun <E: Throwable, R, R2> Try<E, (R) -> R2>
        .apply(f: Try<E, R>): Try<E, R2> = when(this) {
            is Try.Failure -> Try.Failure(error)
            is Try.Success -> f.map(result)
}

/**
 * flatMap/bind/chain/liftM
 */
infix fun <E: Throwable, R, R2> Try<E, R>
        .flatMap(f: (R) -> Try<E, R2>): Try<E, R2> = when(this) {
            is Try.Failure -> Try.Failure(error)
            is Try.Success -> f(result)
}

/**
 * mapLeft
 */
infix fun <E: Throwable, E2: Throwable, R> Try<E, R>
        .mapLeft(f: (E) -> E2): Try<E2, R> = when(this) {
            is Try.Failure -> Try.Failure(f(error))
            is Try.Success -> Try.Success(result)
}

/**
 * fold/either
 */
fun <E: Throwable, R, A> Try<E, R>
        .fold(f: (E) -> A, r: (R) -> A): A = when(this) {
            is Try.Failure -> f(error)
            is Try.Success -> r(result)
}