package com.miruken.callback

import com.miruken.typeOf

class TargetActionBuilder<T>(val handler: Handling) {
    inline operator fun <reified A> invoke(
            crossinline block: T.(a: A) -> Unit
    ) : (T.() -> Unit)? {
        val args = handler.resolveArgs(typeOf<A>())
        return if (args != null) {
            { block(args[0] as A) }
        } else null
    }

    inline operator fun <reified A1, reified A2> invoke(
            crossinline block: T.(a1: A1, a2: A2) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>())?.let {
                { block(it[0] as A1, it[1] as A2) }
            }

    inline operator fun <reified A1, reified A2, reified A3> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>())?.let {
                { block(it[0] as A1, it[1] as A2, it[2] as A3) }
            }

    inline operator fun <reified A1, reified A2, reified A3, reified A4> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>())?.let {
                { block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4) }
            }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>())?.let {
                { block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5) }
            }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>())?.let {
                { block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6) }
            }

    inline operator fun <reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7> invoke(
            crossinline block: T.(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) -> Unit
    ) : (T.() -> Unit)? =
            handler.resolveArgs(typeOf<A1>(), typeOf<A2>(), typeOf<A3>(), typeOf<A4>(), typeOf<A5>(), typeOf<A6>(), typeOf<A7>())?.let {
                { block(it[0] as A1, it[1] as A2, it[2] as A3, it[3] as A4, it[4] as A5, it[5] as A6, it[6] as A7) }
            }
}

inline fun <reified T> Handling.target() = TargetActionBuilder<T>(this)

