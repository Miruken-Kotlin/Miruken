package com.miruken.mvc

import com.miruken.callback.notHandled
import com.miruken.mvc.view.View
import com.miruken.mvc.view.ViewLayer
import com.miruken.mvc.view.ViewRegion
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance

class TestViewRegion : ViewRegion {
    override fun view(viewKey: Any, init: (View.() -> Unit)?) =
        ((viewKey as? KType)?.classifier as? KClass<*>)?.run {
            createInstance() as? View
        } ?: notHandled()

    override fun show(view: View): ViewLayer {
        notHandled()
    }
}