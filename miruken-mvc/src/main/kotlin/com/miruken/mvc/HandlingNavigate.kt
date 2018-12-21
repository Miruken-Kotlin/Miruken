package com.miruken.mvc

import com.miruken.callback.*
import com.miruken.context.Context
import com.miruken.typeOf

inline fun <reified C: Controller> Handling.next(): TargetActionBuilder<C> =
        navigate(NavigationStyle.NEXT)

inline fun <reified C: Controller> Handling.next(
        noinline action: (C) -> Unit
): TargetAction<C> = navigate<C>(NavigationStyle.NEXT)(action)

inline fun <reified C: Controller> Handling.push(): TargetActionBuilder<C> =
        navigate(NavigationStyle.PUSH)

inline fun <reified C: Controller> Handling.push(
        noinline action: (C) -> Unit
): TargetAction<C> = navigate<C>(NavigationStyle.PUSH)(action)

inline fun <reified C: Controller> Handling.partial(): TargetActionBuilder<C> =
        navigate(NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.partial(
        noinline action: (C) -> Unit
): TargetAction<C> = navigate<C>(NavigationStyle.PARTIAL)(action)

inline fun <reified C: Controller> Handling.fork(join: Context): TargetActionBuilder<C> =
        navigate(NavigationStyle.FORK, join)

inline fun <reified C: Controller> Handling.fork(
        join: Context,
        noinline action: (C) -> Unit
): TargetAction<C> = navigate<C>(NavigationStyle.FORK, join)(action)

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
): TargetAction<C> = navigate<C>(style, join)(action)

fun Handling.goBack() {
    handle(Navigation.GoBack()) otherwise {
        error("Navigation backwards not handled")
    }
}