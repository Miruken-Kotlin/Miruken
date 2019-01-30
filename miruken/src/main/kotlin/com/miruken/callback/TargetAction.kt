package com.miruken.callback

import com.miruken.typeOf

typealias TargetAction<T> = T.(handler: Handling) -> Boolean

inline fun <reified T, R> targetAction(
        noinline notify: (TargetAction<T>) -> R
) = TargetActionBuilder(notify)

class TargetActionBuilder<T, R>(val notify: (TargetAction<T>) -> R) {
    inline operator fun <S> invoke(
            crossinline block: T.() -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { s = block(); true } to s
    }

    inline operator fun <reified A, S> invoke(
            crossinline block: T.(a: A) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A>())?.also {
                s = block(it[0] as A)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, S> invoke(
            crossinline block: T.(a1: A1, a2: A2) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>())?.also {
                s = block(it[0] as A1, it[1] as A2)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, reified A3, S> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>())?.also {
                s = block(it[0] as A1, it[1] as A2, it[2] as A3)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, S> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>())?.also {
                s = block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, S> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>())?.also {
                s = block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, S> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>())?.also {
                s = block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6)
            } != null
        } to s
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, S> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) -> S
    ): Pair<R, S?> {
        var s: S? = null
        return notify { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>(), typeOf<A7>())?.also {
                s = block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6, it[6] as A7)
            } != null
        } to s
    }
}


