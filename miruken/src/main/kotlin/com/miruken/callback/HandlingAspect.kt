package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.unwrap
import com.miruken.runtime.PROMISE_TYPE
import kotlin.reflect.full.isSubtypeOf

typealias AspectBeforeBlock = (Any, Handling) -> Any?
typealias AspectAfterBlock  = (Any, Handling, Any?) -> Unit

fun Handling.aspectBefore(
        before:    AspectBeforeBlock?,
        reentrant: Boolean = false
) : Handling = aspect(before, null, reentrant)

fun Handling.aspectAfter(
        after:     AspectAfterBlock?,
        reentrant: Boolean = false
) : Handling = aspect(null, after, reentrant)

fun Handling.aspect(
        before:    AspectBeforeBlock?,
        after:     AspectAfterBlock?,
        reentrant: Boolean = false
) : Handling {
    if (before == null && after == null) return this
    return filter(reentrant) { callback, _, composer, proceed ->
        val cb    = callback as? Callback
        val state = before?.let { b ->
            val state = b(callback, composer)
            if (state is Promise<*>) {
                // TODO("Use Promise.wait if cb.resultType is not a Promise")
                // TODO("or you will get a ClassCastException")
                cb?.result = state.then { st ->
                    if (st != false) {
                        aspectProceed(callback, composer, proceed, after, state)
                        cb?.result?.let { Promise.resolve(it) } ?: Promise.EMPTY
                    } else {
                        Promise.reject(RejectedException(callback))
                    }
                }.unwrap()
                return@filter HandleResult.HANDLED
            }
            if (state == false) {
                if (cb?.resultType?.isSubtypeOf(PROMISE_TYPE) == true) {
                    cb.result = Promise.reject(RejectedException(callback))
                    return@filter HandleResult.HANDLED
                }
                throw RejectedException(callback)
            }
            state
        }
        aspectProceed(callback, composer, proceed, after, state)
    }
}

private fun aspectProceed(
        callback: Any,
        composer: Handling,
        proceed:  () -> HandleResult,
        after:    AspectAfterBlock?,
        state:    Any?
) : HandleResult {
    if (after == null) return proceed()
    var promise: Promise<*>? = null
    try {
        val result = proceed()
        promise = (callback as? Callback)?.let {
            it.result as? Promise<*>
        }?.finally { after(callback, composer, state) }
        return result
    } finally {
        if (promise == null)
            after(callback, composer, state)
    }
}