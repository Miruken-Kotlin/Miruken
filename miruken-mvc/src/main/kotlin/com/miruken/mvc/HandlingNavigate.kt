package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.callback.TargetActionBuilder
import com.miruken.callback.handle
import com.miruken.typeOf

inline fun <reified C: Controller> Handling.next() =
        navigate<C>(NavigationStyle.NEXT)

inline fun <reified C: Controller> Handling.next(
        noinline action: C.() -> Unit
) = (navigate<C>(NavigationStyle.NEXT)) { action() }

inline fun <reified C: Controller> Handling.push() =
        navigate<C>(NavigationStyle.PUSH)

inline fun <reified C: Controller> Handling.push(
        noinline action: C.() -> Unit
) = (navigate<C>(NavigationStyle.PUSH)) { action() }

inline fun <reified C: Controller> Handling.partial() =
        navigate<C>(NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.partial(
        noinline action: C.() -> Unit
) = (navigate<C>(NavigationStyle.PARTIAL)) { action() }

inline fun <reified C: Controller> Handling.navigate(
        style:  NavigationStyle
) = TargetActionBuilder<C> { action ->
    val navigation = Navigation(typeOf<C>(), action, style)
    handle(navigation) failure {
        error("Navigation $style to ${C::class} not handled")
    }
}

inline fun <reified C: Controller> Handling.navigate(
        style:           NavigationStyle,
        noinline action: C.() -> Unit
) = (navigate<C>(style)) { action() }

fun Handling.goBack() {
    handle(Navigation.GoBack()) failure {
        error("Navigation backwards not handled")
    }
}