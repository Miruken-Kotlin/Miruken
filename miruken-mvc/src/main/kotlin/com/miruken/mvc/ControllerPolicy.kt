package com.miruken.mvc

import com.miruken.mvc.policy.DefaultPolicy
import com.miruken.mvc.view.View
import java.lang.ref.WeakReference

class ControllerPolicy(controller: Controller) : DefaultPolicy() {
    private val _controller = WeakReference(controller)

    init {
        track()
    }

    val controller: Controller? get() = _controller.get()
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

fun Controller.dependsOn(dependency: View): Controller {
    policy.addDependency(dependency.policy)
    return this
}

fun Controller.dependsOn(dependency: Controller): Controller {
    policy.addDependency(dependency.policy)
    return this
}
