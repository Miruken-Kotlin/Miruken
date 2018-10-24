package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.callback.handle
import com.miruken.mvc.option.pushLayer
import com.miruken.typeOf

inline fun <reified C: Controller> Handling.next(
        noinline action: C.() -> Unit
) = navigate(action, NavigationStyle.NEXT)

inline fun <reified C: Controller> Handling.push(
        noinline action: C.() -> Unit
) = navigate(action, NavigationStyle.PUSH)

inline fun <reified C: Controller> Handling.partial(
        noinline action: C.() -> Unit
) = navigate(action, NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.navigate(
        noinline action: C.() -> Unit,
                 style:  NavigationStyle
) {
    val navigation = Navigation(typeOf<C>(), action, style)
    val handler    = when (style) {
        NavigationStyle.PUSH -> pushLayer
        else -> this
    }
    handler.handle(navigation) otherwise  {
        error("Navigation $style to ${C::class} not handled")
    }
}

fun Handling.goBack() {
    val goBack = GoBack()
    handle(goBack) otherwise {
        error("Navigation backwards not handled")
    }
}