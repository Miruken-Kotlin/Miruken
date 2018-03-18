package com.miruken.mvc

import com.miruken.container.Container
import com.miruken.mvc.policy.DefaultPolicy
import com.miruken.mvc.view.View
import com.miruken.protocol.proxy
import java.lang.ref.WeakReference

class ControllerPolicy(controller: Controller) : DefaultPolicy() {
    private val _controller = WeakReference(controller)

    init {
        track()
    }

    val controller: Controller? get() = _controller.get()

    fun autoRelease(): ControllerPolicy {
        autoRelease {
            controller?.run {
                context?.proxy<Container>()?.release(this)
            }
        }
        return this
    }
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
