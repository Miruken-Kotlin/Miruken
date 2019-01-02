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
    var controller: C? by weak()
        private set

    var viewLayer: ViewingLayer? by weak()

    var back: Navigation<*>? = null

    override val canFilter = false

    fun invokeOn(controller: C): Boolean {
        this.controller = controller
        return controller.action(controller.context!!)
    }

    class GoBack
}

val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
