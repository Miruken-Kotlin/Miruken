package com.miruken.mvc.policy

import com.miruken.concurrent.Promise
import java.lang.ref.WeakReference

class PromisePolicy(promise: Promise<*>) : DefaultPolicy() {
    private val _promise = WeakReference(promise)

    init { track() }

    val promise get() = _promise.get()

    fun autoRelease(): PromisePolicy {
        autoRelease {
            promise?.run { cancel() }
        }
        return this
    }
}