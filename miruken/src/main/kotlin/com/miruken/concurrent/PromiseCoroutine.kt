package com.miruken.concurrent

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Promise<T>.await(): T =
        suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
            cont.invokeOnCancellation { cancel() }
            then<Unit, Unit>(cont::resume, cont::resumeWithException)
            cancelled { cont.cancel(it) }
        }