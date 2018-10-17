package com.miruken.mvc

import com.miruken.mvc.policy.DefaultPolicy
import com.miruken.mvc.view.Viewing
import java.lang.ref.WeakReference

class ControllerPolicy(controller: Controller) : DefaultPolicy() {
    private val _controller = WeakReference(controller)

    init {
        track()
    }

    val controller get() = _controller.get()
}


fun Controller.track(): Controller {
    policy.track()
    return this
}

fun Controller.retain(): Controller {
    policy.retain()
    return this
}

fun Controller.release(): Controller {
    policy.release()
    return this
}

fun Controller.dependsOn(dependency: Viewing): Controller {
    policy.addDependency(dependency.policy)
    return this
}

fun Controller.dependsOn(dependency: Controller): Controller {
    policy.addDependency(dependency.policy)
    return this
}
