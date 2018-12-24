package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.callback.TargetAction
import com.miruken.callback.TargetActionBuilder
import com.miruken.callback.handle
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
): TargetAction<C> = navigate(NavigationStyle.PUSH, action)

inline fun <reified C: Controller> Handling.push(
        noinline action: (C) -> Unit,
        noinline join:   (Context) -> Unit
): TargetAction<C> = navigate(NavigationStyle.PUSH, action, join)

inline fun <reified C: Controller> Handling.partial(): TargetActionBuilder<C> =
        navigate(NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.partial(
        noinline action: (C) -> Unit
): TargetAction<C> = navigate<C>(NavigationStyle.PARTIAL)(action)

inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle
): TargetActionBuilder<C> {
    return TargetActionBuilder { action ->
        val navigation = Navigation(typeOf<C>(), action, style)
        handle(navigation) otherwise {
            error("Navigation $style to ${C::class} not handled")
        }
    }
}

inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle,
        noinline action: (C) -> Unit,
        noinline join:   ((Context) -> Unit)? = null
): TargetAction<C> = TargetActionBuilder<C> { a ->
        val navigation = Navigation(typeOf<C>(), a, style, join)
        handle(navigation) otherwise {
            error("Navigation $style to ${C::class} not handled")
        }
    }(action)

fun Handling.goBack() {
    handle(Navigation.GoBack()) otherwise {
        error("Navigation backwards not handled")
    }
}