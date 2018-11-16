package com.miruken.mvc

import com.miruken.TypeReference
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
        val controllerType: TypeReference,
        val action:         TargetAction<C>,
        val style:          NavigationStyle
): FilteringCallback {
    override val canFilter = false

    private var _controller: WeakReference<C>? = null

    var back: Navigation<*>? = null

    val controller get() = _controller?.get()

    fun invokeOn(controller: C): Boolean {
        _controller = WeakReference(controller)
        return controller.action(controller.context!!)
    }

    class GoBack
}

val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
