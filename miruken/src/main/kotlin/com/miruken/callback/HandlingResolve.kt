package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.filterIsAssignableTo
import com.miruken.runtime.typeOf
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
fun Handling.resolveAsync(key: Any) : Promise<Any?> {
    val inquiry = key as? Inquiry ?: Inquiry(key)
    inquiry.wantsAsync = true
    return handle(inquiry) success {
        inquiry.result as? Promise<Any>
    } ?: Promise.EMPTY
}

inline fun <reified T: Any> Handling.resolve() : T? =
        resolve(typeOf<T>()) as? T

inline fun <reified T: Any> Handling.resolveAsync() : Promise<T?> =
        resolveAsync(typeOf<T>()) then { it as? T }

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
                else -> Promise.EMPTY_LIST
            }
        }
    } ?: Promise.EMPTY_LIST
}

inline fun <reified T: Any> Handling.resolveAll() : List<T> =
    resolveAll(typeOf<T>()).filterIsInstance<T>()

inline fun <reified T: Any> Handling.resolveAllAsync() : Promise<List<T>> =
        resolveAllAsync(typeOf<T>()) then { it.filterIsInstance<T>() }

inline fun <reified T: Any> Handling.provide(value: T) =
        Provider(value, typeOf<T>()) + this

private fun List<Any>.coerceList(key: Any) : List<Any> {
    return when (key) {
        is KType, is KClass<*>, is Class<*> -> filterIsAssignableTo(key)
        else -> this
    }
}