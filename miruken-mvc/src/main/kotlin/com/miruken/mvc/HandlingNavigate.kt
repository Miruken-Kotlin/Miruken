package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.callback.TargetActionBuilder
import com.miruken.callback.handle
import com.miruken.callback.resolve
import com.miruken.context.Context
import com.miruken.typeOf

inline fun <reified C: Controller> Handling.next() =
        navigate<C>(NavigationStyle.NEXT)

inline fun <reified C: Controller> Handling.next(
        noinline action: (C) -> Unit
) = navigate<C>(NavigationStyle.NEXT)(action)

inline fun <reified C: Controller> Handling.push() =
        navigate<C>(NavigationStyle.PUSH)

inline fun <reified C: Controller> Handling.push(
        noinline action: (C) -> Unit
) = navigate<C>(NavigationStyle.PUSH)(action)

inline fun <reified C: Controller> Handling.partial() =
        navigate<C>(NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.partial(
        noinline action: (C) -> Unit
) = navigate<C>(NavigationStyle.PARTIAL)(action)

inline fun <reified C: Controller> Handling.fork(join: Context) =
        navigate<C>(NavigationStyle.FORK, join)

inline fun <reified C: Controller> Handling.fork(
        join: Context,
        noinline action: (C) -> Unit
) = navigate<C>(NavigationStyle.FORK, join)(action)

inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle,
        join:  Context? = null
): TargetActionBuilder<C> {
    val from = resolve<Context>() ?: error(
            "Navigation $style requires a context")
    return TargetActionBuilder { action ->
        val navigation = Navigation(typeOf<C>(), action, style, from, join)
        handle(navigation) otherwise {
            error("Navigation $style to ${C::class} not handled")
        }
    }
}

inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle,
        join:  Context? = null,
        noinline action: (C) -> Unit
) = navigate<C>(style, join)(action)

fun Handling.goBack() {
    handle(Navigation.GoBack()) otherwise {
        error("Navigation backwards not handled")
    }
}