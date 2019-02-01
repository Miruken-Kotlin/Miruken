package com.miruken.mvc

import com.miruken.callback.FilteringCallback
import com.miruken.callback.Handling
import com.miruken.callback.TargetAction
import com.miruken.mvc.view.ViewingLayer
import com.miruken.weak

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
    class GoBack

    var controller: C? by weak()
        private set

    var viewLayer: ViewingLayer? by weak()

    var noBack = false

    var back: Navigation<*>? = null

    override val canFilter = false

    val context get() = controller?.context

    fun invokeOn(controller: C): Boolean {
        val context = controller.context
        checkNotNull(context) {
            "Controller invocation requires a context"
        }
        this.controller = controller
        return GLOBAL_EXECUTE.all { it(this) } && controller.action(context)
    }

    companion object {
        val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
        val GLOBAL_EXECUTE = mutableListOf<(Navigation<*>) -> Boolean>()
    }
}

