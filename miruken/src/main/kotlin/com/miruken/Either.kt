package com.miruken

sealed class Either<out L, out R> {
    data class Left<out L>(val left: L)   : Either<L, Nothing>()
    data class Right<out R>(val right: R) : Either<Nothing, R>()
}

/**
 * seq
 */
infix fun <L, R, R2> Either<L, R>
        .seq(e: Either<L, R2>): Either<L, R2> = e

/**
 * map/fmap
 */
infix fun <L, R, R2> Either<L, R>
        .map(f: (R) -> R2): Either<L, R2> = when (this) {
            is Either.Left -> this
            is Either.Right -> Either.Right(f(right))
}

/**
 * apply/<*>/ap
 */
infix fun <L, R, R2> Either<L, (R) -> R2>
        .apply(f: Either<L, R>): Either<L, R2> = when(this) {
            is Either.Left -> this
            is Either.Right -> f.map(right)
}

/**
 * flatMap/bind/chain/liftM
 */
infix fun <L, R, R2> Either<L, R>
        .flatMap(f: (R) -> Either<L, R2>): Either<L, R2> = when(this) {
            is Either.Left -> this
            is Either.Right -> f(right)
}

/**
 * mapLeft
 */
infix fun <L, L2, R> Either<L, R>
        .mapLeft(f: (L) -> L2): Either<L2, R> = when(this) {
            is Either.Left -> Either.Left(f(left))
            is Either.Right -> this
}

/**
 * fold/either
 */
fun <L, R, A> Either<L, R>
        .fold(l: (L) -> A, r: (R) -> A): A = when(this) {
            is Either.Left -> l(left)
            is Either.Right -> r(right)
}