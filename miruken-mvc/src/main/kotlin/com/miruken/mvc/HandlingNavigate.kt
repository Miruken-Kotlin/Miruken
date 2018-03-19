package com.miruken.mvc

import com.miruken.callback.Handling

inline fun <reified C: Controller> Handling.next(
        noinline action: C.() -> Unit
) = Navigate(this).navigate(NavigationStyle.NEXT, action)

inline fun <reified C: Controller> Handling.push(
        noinline action: C.() -> Unit
) = Navigate(this).navigate(NavigationStyle.PUSH, action)

inline fun <reified C: Controller> Handling.navigate(
        style:  NavigationStyle,
        noinline action: C.() -> Unit
) = Navigate(this).navigate(style, action)

fun Handling.goBack() = Navigate(this).goBack()