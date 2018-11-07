package com.miruken.mvc

import com.miruken.TypeReference
import com.miruken.callback.notHandled
import com.miruken.event.Event
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingLayer
import com.miruken.mvc.view.ViewingRegion
import com.miruken.mvc.view.ViewingStackView
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.jvmErasure

class TestViewRegion : ViewingStackView {
    override var viewModel: Any? = null

    override fun createViewStack() = TestViewRegion()

    override fun view(viewKey: Any, init: (Viewing.() -> Unit)?) =
            (when (viewKey) {
                is KType -> viewKey.jvmErasure.createInstance()
                is KClass<*> -> viewKey.createInstance()
                is TypeReference -> (viewKey.type as Class<*>).newInstance()
                else -> null
            } as? Viewing) ?: notHandled()

    override fun show(view: Viewing) = Layer(view)

    override fun display(region: ViewingRegion) =
            region.show(this)

    override fun pushLayer() = Layer(this)

    override fun unwindLayers() {}

    class Layer(val view: Viewing) : ViewingLayer {
        override val index        = 0
        override val transitioned = Event<ViewingLayer>()
        override val closed       = Event<ViewingLayer>()

        override fun duration(
                duration: Duration,
                complete: (Boolean) -> Unit
        ) = {}

        override fun close() {}
    }
}