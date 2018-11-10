package com.miruken.mvc.view

import com.miruken.callback.Handling
import com.miruken.context.Context
import com.miruken.mvc.Controller
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.push
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface ViewingRegion {
    fun view(
            viewKey: Any,
            init:    (Viewing.() -> Unit)? = null
    ): Viewing

    fun createViewStack(): ViewingStackView

    fun show(view: Viewing): ViewingLayer

    companion object {
        val PROTOCOL = typeOf<ViewingRegion>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as ViewingRegion
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified V: Viewing> ViewingRegion.view(
        noinline init: (V.() -> Unit)? = null
) = view(typeOf<V>(), init as (Viewing.() -> Unit)?) as V

@Suppress("UNCHECKED_CAST")
inline fun <reified V: Viewing> ViewingRegion.show(
        noinline init: (V.() -> Unit)? = null
) = show(view(typeOf<V>(), init as (Viewing.() -> Unit)?))

inline fun <reified C: Controller> Handling.region(
        crossinline action: (C: Controller) -> Unit
): Viewing = object : ViewAdapter() {
    override fun display(region: ViewingRegion): ViewingLayer {
        lateinit var layer: ViewingLayer
        val stack = region.createViewStack()
        (push<C>()) {
            val controllerContext = context ?:
                    error("Region navigation seemed to have failed")
            controllerContext.addHandlers(stack)
            action(this)
            layer = stack.display(ViewingRegion(this@region.pushLayer))
            layer.closed += { controllerContext.end() }
            controllerContext.contextEnded += { layer.close() }
        }
        return layer
    }
}

fun Context.addRegion(region: ViewingRegion): Context {
    val child = createChild()
    child.addHandlers(region)
    child.contextEnded += { (ctx, _) -> ctx.removeHandlers(region)}
    return child
}
