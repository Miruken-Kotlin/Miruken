package com.miruken.mvc.view

import com.miruken.mvc.policy.DefaultPolicy
import java.lang.ref.WeakReference

class ViewPolicy(view: Viewing) : DefaultPolicy() {
    private val _view = WeakReference(view)

    val view get() = _view.get()
}

fun Viewing.track(): Viewing {
    policy.track()
    return this
}

fun Viewing.retain(): Viewing {
    policy.retain()
    return this
}

fun Viewing.release(): Viewing {
    policy.release()
    return this
}

fun Viewing.dependsOn(dependency: Viewing): Viewing {
    policy.addDependency(dependency.policy)
    return this
}

fun Viewing.doesDependOn(dependency: Viewing) =
        policy.isDependency(dependency.policy)
