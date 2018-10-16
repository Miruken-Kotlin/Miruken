package com.miruken.context

import com.miruken.callback.*
import com.miruken.concurrent.Promise

fun Handling.trackPromise(context: Context) =
        filter { callback, _, _, proceed ->
            proceed().also { result -> result success {
                val cb = callback as? Callback
                (cb?.result as? Promise<*>)?.also { promise ->
                    if (context.state == ContextState.ACTIVE) {
                        promise.finally(context.contextEnded
                                .register { _ -> promise.cancel() })
                    } else {
                        promise.cancel()
                    }
                }
            }}
        }

val Handling.publishFromRoot get() =
    resolve<Context>()?.root?.publish
            ?: error("The root context could not be found")