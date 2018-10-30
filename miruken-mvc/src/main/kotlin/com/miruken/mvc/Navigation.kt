package com.miruken.mvc

import com.miruken.callback.FilteringCallback
import com.miruken.callback.Handling
import java.lang.ref.WeakReference

enum class NavigationStyle {
    NEXT,
    PUSH,
    PARTIAL
}

class Navigation<C: Controller>(
        val controllerKey: Any,
        val action:        C.() -> Unit,
        val style:         NavigationStyle
): FilteringCallback {
    override val canFilter = false

    private var _controller: WeakReference<C>? = null

    var back: Navigation<*>? = null

    val controller get() = _controller?.get()

    fun invokeOn(controller: C) {
        _controller = WeakReference(controller)
        controller.action()
    }

    class GoBack

    companion object {
        val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
    }
}

