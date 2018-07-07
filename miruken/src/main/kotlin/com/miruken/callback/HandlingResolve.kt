package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.typeOf

val Handling.resolving get() = ResolvingHandler(this)

val Handling.resolvingAll get() = CallbackSemanticsHandler(
        ResolvingHandler(this), CallbackOptions.BROADCAST)

fun Handling.resolve(key: Any): Any? {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    return handle(inquiry) success { inquiry.result }
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAsync(
        key:      Any,
        required: Boolean = false
): Promise<Any?> {
    val inquiry = key as? Inquiry ?: Inquiry(key)
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

inline fun <reified T: Any> Handling.resolve(): T? =
        resolve(typeOf<T>()) as? T

inline fun <reified T: Any> Handling.resolveAsync(): Promise<T?> =
        resolveAsync(typeOf<T>()) then { it as? T }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAll(key: Any): List<Any> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true)
    return handle(inquiry, true) success  {
        inquiry.result as? List<Any>
    } ?: emptyList()
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAllAsync(key: Any): Promise<List<Any>> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true)
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

inline fun <reified T: Any> Handling.resolveAll(): List<T> =
    resolveAll(typeOf<T>()).filterIsInstance<T>()

inline fun <reified T: Any> Handling.resolveAllAsync(): Promise<List<T>> =
        resolveAllAsync(typeOf<T>()) then { it.filterIsInstance<T>() }
