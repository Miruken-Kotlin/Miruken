package com.miruken.concurrent

import kotlinx.coroutines.Deferred
import java.util.concurrent.CancellationException

fun <T> Deferred<T>.asPromise() = Promise<T> {
    resolve, reject, cancel ->
        cancel { cancel() }
        invokeOnCompletion { cause ->
            if (cause is kotlinx.coroutines.CancellationException) {
                reject(CancellationException(cause.message))
            } else {
                try {
                    resolve(getCompleted())
                } catch (t: Throwable) {
                    reject(t)
                }
            }
        }
    }