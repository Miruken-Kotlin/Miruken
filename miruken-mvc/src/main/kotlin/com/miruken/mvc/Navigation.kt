package com.miruken.mvc

import com.miruken.callback.FilteringCallback
import com.miruken.callback.Handling
import com.miruken.callback.TargetAction
import com.miruken.context.Context
import java.lang.ref.WeakReference

enum class NavigationStyle {
    NEXT,
    PUSH,
    PARTIAL
}

class Navigation<C: Controller>(
        val controllerKey: Any,
        val action:        TargetAction<C>,
        val style:         NavigationStyle,
        val join:          ((Context) -> Unit)? = null
): FilteringCallback {
    private var _controller: WeakReference<C>? = null

    val controller get() = _controller?.get()

    var back: Navigation<*>? = null

    override val canFilter = false

    init {
        require(join == null || style == NavigationStyle.PUSH) {
            "Navigation $style received a join argument"
        }
    }

    fun invokeOn(controller: C): Boolean {
        _controller = WeakReference(controller)
        return controller.action(controller.context!!)
    }

    class GoBack
}

val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
