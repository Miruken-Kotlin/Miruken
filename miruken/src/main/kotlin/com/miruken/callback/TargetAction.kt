package com.miruken.callback

import com.miruken.typeOf

typealias TargetAction<T> = T.(handler: Handling) -> Boolean

inline fun <reified T> targetAction(
        noinline notify: ((TargetAction<T>) -> Unit)? = null
) = TargetActionBuilder(notify)

class TargetActionBuilder<T>(
        val notify: ((TargetAction<T>) -> Unit)? = null
) {
    inline operator fun invoke(
            crossinline block: T.() -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { block(); true }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A> invoke(
            crossinline block: T.(a: A) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A>())?.let {
                block(it[0] as A); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2> invoke(
            crossinline block: T.(a1: A1, a2: A2) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>())?.let {
                block(it[0] as A1, it[1] as A2); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2, reified A3> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>())?.let {
                block(it[0] as A1, it[1] as A2, it[2] as A3); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>())?.let {
                block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>())?.let {
                block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>())?.let {
                block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) -> Unit
    ): TargetAction<T> {
        val ta: TargetAction<T> = { handler ->
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>(), typeOf<A7>())?.let {
                block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6, it[6] as A7); true
            } ?: false
        }
        notify?.invoke(ta)
        return ta
    }
}


