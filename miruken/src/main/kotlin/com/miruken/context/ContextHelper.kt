package com.miruken.context

import com.miruken.callback.*
import com.miruken.concurrent.Promise

val Handling.trackAsync get() = trackPromise(
        (this as? Context) ?: resolve()
            ?: error("Async support requires a Context")
)

fun Handling.trackPromise(context: Context) =
        filter { callback, _, _, proceed ->
            proceed().also { result -> result success {
                val cb = callback as? Callback
                (cb?.result as? Promise<*>)?.also { promise ->
                    context.track(promise)
                }
            }}
        }

fun Handling.track(promise: Promise<*>): Handling {
    ((this as? Context) ?: resolve()
        ?: error("Tracking support requires a Context")).also {
        if (it.state == ContextState.ACTIVE) {
            promise.finally(it.contextEnded
                    .register { promise.cancel() })
        } else {
            promise.cancel()
        }
    }
    return this
}

fun Handling.dispose(closeable: AutoCloseable): Handling {
    ((this as? Context) ?: resolve()
        ?: error("Disposal support requires a Context")).also {
        it.contextEnded += {
            try {
                closeable.close()
            } catch (e: Throwable) {
                // don't care
            }
        }
    }
    return this
}

val Handling.publishFromRoot: Handling get() =
    resolve<Context>()?.root?.publish
            ?: error("The root context could not be found")

tailrec fun Context.parent(howMany: Int): Context? =
        when {
            howMany == 0 -> this
            howMany < 0 -> null
            else -> parent?.parent(howMany - 1)
        }

tailrec fun Context.deepest(): Context =
        takeUnless { it.hasChildren } ?: children.last().deepest()