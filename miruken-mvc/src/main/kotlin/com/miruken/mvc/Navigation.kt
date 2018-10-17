package com.miruken.mvc

import com.miruken.callback.Handling
import java.lang.ref.WeakReference

enum class NavigationStyle { NEXT, PUSH }

class Navigation<C: Controller>(
        val controllerKey: Any,
        val action:        C.() -> Any?,
        val style:         NavigationStyle
) {
    private var _controller: WeakReference<C>? = null
    private var _result: Any? = null

    var back: Navigation<*>? = null

    val controller get() = _controller?.get()

    fun clearResult(): Any? {
        val result = _result
        _result    = null
        return result
    }

    fun invokeOn(controller: C): Any? {
        _controller = WeakReference(controller)
        _result     = controller.action()
        return _result
    }


    companion object {
        val GLOBAL_PREPARE = mutableListOf<(Handling) -> Handling>()
    }
}