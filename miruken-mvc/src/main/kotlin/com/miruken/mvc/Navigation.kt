package com.miruken.mvc

import com.miruken.callback.FilteringCallback
import com.miruken.callback.Handling
import com.miruken.callback.TargetAction
import com.miruken.context.Context
import java.lang.ref.WeakReference

enum class NavigationStyle {
    NEXT,
    PUSH,
    PARTIAL,
    FORK
}

class Navigation<C: Controller>(
        val controllerKey: Any,
        val action:        TargetAction<C>,
        val style:         NavigationStyle,
        from:              Context,
        join:              Context? = null
): FilteringCallback {
    override val canFilter = false

    private val _from = WeakReference<Context>(from)
    private val _join = join?.let { WeakReference(it) }
    private var _controller: WeakReference<C>? = null

    var back: Navigation<*>? = null

    val from       get() = _from.get()
    val join       get() = _join?.get()
    val controller get() = _controller?.get()

    init {
        if (style == NavigationStyle.FORK) {
            require(join != null) {
                "Navigation $style requires a join argument"
            }
        } else {
            require(join == null) {
                "Navigation $style received a join argument"
            }
        }
    }

    fun invokeOn(controller: C): Boolean {
        _controller = WeakReference(controller)
        return controller.action(controller.context!!)
    }

    class GoBack
}

val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
