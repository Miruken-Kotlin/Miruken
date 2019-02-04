package com.miruken.error

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.flatten
import com.miruken.fold
import java.util.concurrent.CancellationException

val Handling.recover get() = recover()

fun Handling.recover(context: Any? = null): Handling {
    return filter { callback, _, composer, proceed ->
        if (callback is Composition) proceed()
        else {
            fun promisify(e: Throwable): Promise<*> {
                return when (e) {
                    is CancellationException ->
                        Promise.reject(e)
                    else -> Errors(composer)
                            .handleException(e, callback, context)
                }
            }
            val cb = callback as? Callback
            try {
                val result = proceed()
                if (result.handled) {
                    (cb?.result as? Promise<*>)?.also { p ->
                        cb.result = (p.catch(::promisify) then { r ->
                            r.fold({ it }, { Promise.resolve(it) })
                        }).flatten()
                    }
                }
                result
            } catch (e: Throwable) {
                if (cb?.resultType is Promise<*>) {
                    cb.result = promisify(e)
                    HandleResult.NOT_HANDLED
                } else if (e !is CancellationException) {
                    Errors(composer).handleException(e, callback, context)
                }
                HandleResult.HANDLED
            }
        }
    }
}