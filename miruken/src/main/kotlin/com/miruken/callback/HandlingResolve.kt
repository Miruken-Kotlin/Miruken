package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.typeOf

val Handling.infer get() = InferringHandler(this)

val Handling.inferAll get() = CallbackSemanticsHandler(
        InferringHandler(this), CallbackOptions.BROADCAST)

fun Handling.resolve(
        key:    Any,
        parent: Inquiry? = null
): Any? {
    val inquiry = key as? Inquiry ?: Inquiry(key, false, parent)
    return handle(inquiry) success { inquiry.result }
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAsync(
        key:      Any,
        parent:   Inquiry? = null,
        required: Boolean = false
): Promise<Any?> {
    val inquiry = key as? Inquiry ?: Inquiry(key, false, parent)
    inquiry.wantsAsync = true
    return handle(inquiry) success {
        inquiry.result as? Promise<Any>
    } ?: if (required) {
        Promise.reject(IllegalStateException(
                "Promise required a non-null result for key '$key'"))
    } else {
        Promise.EMPTY
    }
}

inline fun <reified T: Any> Handling.resolve(
        parent: Inquiry? = null
): T? = resolve(typeOf<T>(), parent) as? T

inline fun <reified T: Any> Handling.resolveAsync(
        parent: Inquiry? = null
): Promise<T?> =
        resolveAsync(typeOf<T>(), parent) then { it as? T }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAll(
        key:    Any,
        parent: Inquiry? = null
): List<Any> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true, parent)
    return handle(inquiry, true) success  {
        inquiry.result as? List<Any>
    } ?: emptyList()
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAllAsync(
        key:    Any,
        parent: Inquiry? = null
): Promise<List<Any>> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true, parent)
    inquiry.wantsAsync = true
    return handle(inquiry, true) success  {
        inquiry.result?.let {
            when (it) {
                is Promise<*> -> it.then {
                    it as? List<Any> ?: emptyList()
                }
                else -> Promise.EMPTY_LIST
            }
        }
    } ?: Promise.EMPTY_LIST
}

inline fun <reified T: Any> Handling.resolveAll(
        parent: Inquiry? = null
): List<T> =
    resolveAll(typeOf<T>(), parent).filterIsInstance<T>()

inline fun <reified T: Any> Handling.resolveAllAsync(
        parent: Inquiry? = null
): Promise<List<T>> =
        resolveAllAsync(typeOf<T>(), parent) then {
            it.filterIsInstance<T>()
        }
