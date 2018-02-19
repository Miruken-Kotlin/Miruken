package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.filterIsAssignable
import com.miruken.runtime.getKType
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun Handling.resolving() = ResolvingHandler(this)

fun Handling.resolvingAll() = CallbackSemanticsHandler(
        ResolvingHandler(this), CallbackOptions.BROADCAST)

fun Handling.resolve(key: Any) : Any? {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    return handle(inquiry) success {
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
    return handle(inquiry) success  {
        inquiry.result as? Promise<Any>
    } ?: Promise.Empty
}

inline fun <reified T: Any> Handling.resolve() : T? =
        resolve(getKType<T>()) as? T

inline fun <reified T: Any> Handling.resolveAsync() : Promise<T?> =
        resolveAsync(getKType<T>()) then { it as? T }

@Suppress("UNCHECKED_CAST")
fun Handling.resolveAll(key: Any) : List<Any> {
    val inquiry = key as? Inquiry ?: Inquiry(key, true)
    return handle(inquiry, true) success  {
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
    return handle(inquiry, true) success  {
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

inline fun <reified T: Any> Handling.resolveAll() : List<T> =
    resolveAll(getKType<T>()).filterIsInstance<T>()

inline fun <reified T: Any> Handling.resolveAllAsync() : Promise<List<T>> =
        resolveAllAsync(getKType<T>()) then { it.filterIsInstance<T>() }

private fun List<Any>.coerceList(key: Any) : List<Any> {
    return when (key) {
        is KType, is KClass<*>, is Class<*> -> filterIsAssignable(key)
        else -> this
    }
}