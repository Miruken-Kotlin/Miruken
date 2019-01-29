package com.miruken.mvc

import com.miruken.callback.Handling
import com.miruken.callback.TargetActionBuilder
import com.miruken.callback.commandAsync
import com.miruken.concurrent.Promise
import com.miruken.context.Context
import com.miruken.typeOf

typealias TargetActionPromise<C> = TargetActionBuilder<C, Promise<Context>>

inline fun <reified C: Controller> Handling.next(): TargetActionPromise<C> =
        navigate(NavigationStyle.NEXT)

inline fun <reified C: Controller> Handling.next(noinline action: (C) -> Unit) =
        navigate(NavigationStyle.NEXT, action)

inline fun <reified C: Controller> Handling.push(): TargetActionPromise<C> =
        navigate(NavigationStyle.PUSH)

inline fun <reified C: Controller> Handling.push(noinline action: (C) -> Unit) =
        navigate(NavigationStyle.PUSH, action)

inline fun <reified C: Controller> Handling.partial(): TargetActionPromise<C> =
        navigate(NavigationStyle.PARTIAL)

inline fun <reified C: Controller> Handling.partial(noinline action: (C) -> Unit) =
        navigate(NavigationStyle.PARTIAL, action)

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle
): TargetActionPromise<C> {
    return TargetActionBuilder { action ->
        val navigation = Navigation(typeOf<C>(), action, style)
        commandAsync(navigation) as Promise<Context>
    }
}

inline fun <reified C: Controller> Handling.navigate(
        style: NavigationStyle,
        noinline action: (C) -> Unit
): Promise<Context> = navigate<C>(style)(action).first

@Suppress("UNCHECKED_CAST")
fun Handling.goBack() = commandAsync(Navigation.GoBack()) as Promise<Context>