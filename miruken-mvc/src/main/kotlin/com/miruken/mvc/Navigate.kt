package com.miruken.mvc

import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

enum class NavigationStyle { NEXT, PUSH }

@Protocol
interface Navigate {
    fun navigate(
            controllerKey: Any,
            style:         NavigationStyle,
            action:        Controller.() -> Any?
    ): Any?

    fun goBack(): Any?

    companion object {
        val PROTOCOL = typeOf<Navigate>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Navigate
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.next(
        noinline action:  C.() -> Any?
) = navigate(typeOf<C>(), NavigationStyle.NEXT,
        action as Controller.() -> Any?)

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.push(
        noinline action:      C.() -> Any?
) = navigate(typeOf<C>(), NavigationStyle.PUSH,
        action as Controller.() -> Any?)

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.navigate(
        style:                NavigationStyle,
        noinline action:      C.() -> Any?
) = navigate(typeOf<C>(), style,
        action as Controller.() -> Any?)
