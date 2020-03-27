package com.miruken.callback

import com.miruken.callback.policy.bindings.ConstraintBuilder
import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.typeOf
import kotlin.coroutines.coroutineContext

inline fun <reified T: Any> Handling.resolve(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
): T? = resolve(typeOf<T>(), constraints) as? T

fun Handling.resolve(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
): Any? {
    val inquiry = (key as? Inquiry)?.also {
        check(!it.wantsAsync) {
            "Requested Inquiry is asynchronous"
        }
    } ?: Inquiry(key, false)
    constraints?.invoke(ConstraintBuilder(inquiry))
    return handle(inquiry) success { inquiry.result }
}

inline fun <reified T: Any> Handling.resolveAsync(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
): Promise<T?> =
        resolveAsync(typeOf<T>(), constraints) then { it as? T }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAsync(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
): Promise<Any?> {
    val inquiry = (key as? Inquiry)?.also {
        check(it.wantsAsync) {
            "Requested Inquiry is synchronous"
        }
    } ?: Inquiry(key, false).apply { wantsAsync = true }
    constraints?.invoke(ConstraintBuilder(inquiry))
    return handle(inquiry) success {
        inquiry.result as? Promise<Any?>
    } ?: Promise.EMPTY
}

suspend inline fun <reified T: Any> Handling.resolveCo(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
) = resolveCo(typeOf<T>(), constraints)

suspend fun Handling.resolveCo(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
) = with(coroutineContext)
        .resolveAsync(key, constraints).await()

inline fun <reified T: Any> Handling.resolveAll(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
): List<T> =
        resolveAll(typeOf<T>(), constraints).filterIsInstance<T>()

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAll(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
): List<Any> {
    val inquiry = (key as? Inquiry)?.also {
        check(it.many) {
            "Requested Inquiry expects a single result"
        }
        check(!it.wantsAsync) {
            "Requested Inquiry is asynchronous"
        }
    } ?: Inquiry(key, true)
    constraints?.invoke(ConstraintBuilder(inquiry))
    return handle(inquiry, true) success  {
        inquiry.result as? List<Any>
    } ?: emptyList()
}

inline fun <reified T: Any> Handling.resolveAllAsync(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
): Promise<List<T>> =
        resolveAllAsync(typeOf<T>(), constraints) then {
            it.filterIsInstance<T>()
        }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAllAsync(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
): Promise<List<Any>> {
    val inquiry = (key as? Inquiry)?.also {
        check(it.many) {
            "Requested Inquiry expects a single result"
        }
        check(it.wantsAsync) {
            "Requested Inquiry is synchronous"
        }
    } ?: Inquiry(key, true).apply { wantsAsync = true }
    constraints?.invoke(ConstraintBuilder(inquiry))
    return handle(inquiry, true) success  {
        inquiry.result?.let {
            when (it) {
                is Promise<*> -> it.then { r ->
                    r as? List<Any> ?: emptyList()
                }
                else -> Promise.EMPTY_LIST
            }
        }
    } ?: Promise.EMPTY_LIST
}

suspend inline fun <reified T: Any> Handling.resolveAllCo(
        noinline constraints: (ConstraintBuilder.() -> Unit)? = null
) = resolveAllCo(typeOf<T>(), constraints)

suspend fun Handling.resolveAllCo(
        key:         Any,
        constraints: (ConstraintBuilder.() -> Unit)? = null
) = with(coroutineContext)
        .resolveAllAsync(key, constraints).await()