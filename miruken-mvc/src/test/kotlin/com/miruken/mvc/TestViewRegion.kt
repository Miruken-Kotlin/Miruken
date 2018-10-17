package com.miruken.mvc

import com.miruken.callback.notHandled
import com.miruken.mvc.view.Viewing
import com.miruken.mvc.view.ViewingLayer
import com.miruken.mvc.view.ViewingRegion
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance

class TestViewRegion : ViewingRegion {
    override fun view(viewKey: Any, init: (Viewing.() -> Unit)?) =
        ((viewKey as? KType)?.classifier as? KClass<*>)?.run {
            createInstance() as? Viewing
        } ?: notHandled()

    override fun show(view: Viewing): ViewingLayer {
        notHandled()
    }
}