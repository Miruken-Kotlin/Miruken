package com.miruken.mvc.view

import com.miruken.mvc.policy.DefaultPolicy
import java.lang.ref.WeakReference

class ViewPolicy(view: View) : DefaultPolicy() {
    private val _view = WeakReference(view)

    val view: View? get() = _view.get()
}

fun View.track(): View {
    policy.track()
    return this
}

fun View.retain(): View {
    policy.retain()
    return this
}

fun View.release(): View {
    policy.release()
    return this
}

fun View.dependsOn(dependency: View): View {
    policy.addDependency(dependency.policy)
    return this
}

fun View.doesDependOn(dependency: View) =
        policy.isDependency(dependency.policy)
