package com.miruken.mvc

import com.miruken.callback.FilteringCallback
import com.miruken.callback.Handling
import com.miruken.callback.TargetAction
import java.lang.ref.WeakReference

enum class NavigationStyle {
    NEXT,
    PUSH,
    PARTIAL
}

class Navigation<C: Controller>(
        val controllerKey: Any,
        val action:        TargetAction<C>,
        val style:         NavigationStyle
): FilteringCallback {
    private var _controller: WeakReference<C>? = null

    override val canFilter = false

    val controller get() = _controller?.get()

    var back: Navigation<*>? = null

    fun invokeOn(controller: C): Boolean {
        _controller = WeakReference(controller)
        return controller.action(controller.context!!)
    }

    class GoBack
}

val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
