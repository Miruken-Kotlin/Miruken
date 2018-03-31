package com.miruken.mvc.view

import com.miruken.callback.Handling
import com.miruken.context.Context
import com.miruken.mvc.Controller
import com.miruken.mvc.Navigate
import com.miruken.mvc.option.pushLayer
import com.miruken.mvc.push
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface ViewRegion {
    fun view(viewKey: Any, init: (View.() -> Unit)? = null): View
    fun show(view: View): ViewLayer

    companion object {
        val PROTOCOL = typeOf<ViewRegion>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as ViewRegion
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified V: View> ViewRegion.view(
        noinline init: (V.() -> Unit)? = null
) = view(typeOf<V>(), init as (View.() -> Unit)?) as V

@Suppress("UNCHECKED_CAST")
inline fun <reified V: View> ViewRegion.show(
        noinline init: (V.() -> Unit)? = null
) = show(view(typeOf<V>(), init as (View.() -> Unit)?))

inline fun <reified C: Controller> Handling.region(
        crossinline action: (C: Controller) -> Unit
): View = object : ViewAdapter() {
    override fun display(region: ViewRegion): ViewLayer {
        lateinit var layer: ViewLayer
        val stack = region.view<ViewStackView>()
        Navigate(this@region).push<C> {
            val controllerContext = context!!
            controllerContext.addHandlers(stack)
            action(this)
            layer = stack.display(ViewRegion(this@region.pushLayer))
            layer.closed += { _ -> controllerContext.end() }
            controllerContext.contextEnded += { _ -> layer.close() }
        }
        return layer
    }
}

fun Context.addRegion(region: ViewRegion): Context {
    val child = createChild()
    child.addHandlers(region)
    return child
}
