package com.miruken.callback

import com.miruken.concurrent.Promise
import kotlin.reflect.KClass

fun Handling.resolving() = ResolvingHandler(this)

fun Handling.resolvingAll() = CallbackSemanticsHandler(
        ResolvingHandler(this), CallbackOptions.BROADCAST)

fun Handling.resolve(key: Any) : Any? {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    return handle(inquiry) map {
        inquiry.result?.let {
            when (it) {
                is Promise<*> -> it.get()
                else -> it
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAsync(key: Any) : Promise<Any> {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    inquiry.wantsAsync = true
    return handle(inquiry) map {
        inquiry.result as? Promise<Any>
    } ?: Promise.Empty
}

inline fun <reified T> Handling.resolve() : T? =
        resolve(T::class) as? T

inline fun <reified T> Handling.resolveAsync() : Promise<T?> =
        resolveAsync(T::class) then { it as? T }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAll(key: Any) : List<Any> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true)
    return handle(inquiry, true) map {
        inquiry.result?.let {
            when (it) {
                is Promise<*> -> it.get()
                else -> it
            } as? List<Any>
        }?.coerceList(key)
    } ?: emptyList()
}

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAllAsync(key: Any) : Promise<List<Any>> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true)
    inquiry.wantsAsync = true
    return handle(inquiry, true) map {
        inquiry.result?.let {
            when (it) {
                is Promise<*> -> it.then {
                    (it as? List<Any>)?.coerceList(key)
                    ?: emptyList()
                }
                else -> Promise.EmptyList
            }
        }
    } ?: Promise.EmptyList
}

inline fun <reified T> Handling.resolveAll() : List<T> =
    resolveAll(T::class).filterIsInstance<T>()

inline fun <reified T> Handling.resolveAllAsync() : Promise<List<T>> =
        resolveAllAsync(T::class) then { it.filterIsInstance<T>() }

private fun List<Any>.coerceList(key: Any) : List<Any> {
    return when (key) {
        is KClass<*> -> filterIsInstance(key.javaObjectType)
        else -> this
    }
}