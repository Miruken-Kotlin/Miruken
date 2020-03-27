package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.typeOf
import kotlin.coroutines.coroutineContext

inline fun <reified T: Any> Handling.create(): T? =
        create(typeOf<T>()) as? T

fun Handling.create(type: Any): Any? {
    val creation = Creation(type)
    return handle(creation) success { return creation.result }
            ?: throw NotHandledException(creation)
}

inline fun <reified T: Any> Handling.createAsync(): Promise<T?> =
        createAsync(typeOf<T>()) then { it as? T }

fun Handling.createAsync(type: Any): Promise<Any?> {
    val creation = Creation(type).apply {
        wantsAsync = true
    }
    return try {
        handle(creation) failure {
            Promise.reject(NotHandledException(creation))
        } ?: creation.result as Promise<Any?>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

suspend inline fun <reified T: Any> Handling.createCo() =
        createCo(typeOf<T>())

suspend fun Handling.createCo(type: Any) =
        with(coroutineContext).createAsync(type).await()

inline fun <reified T: Any> Handling.createAll(): List<T> =
        createAll(typeOf<T>()).filterIsInstance<T>()

@Suppress("UNCHECKED_CAST")
fun Handling.createAll(type: Any): List<Any> {
    val creation = Creation(type, true)
    return handle(creation) success {
        return creation.result as List<Any>
    } ?: throw NotHandledException(creation)
}

inline fun <reified T: Any> Handling.createAllAsync(): Promise<List<T>> =
        createAllAsync(typeOf<T>()) then {
            it.filterIsInstance<T>()
        }

@Suppress("UNCHECKED_CAST")
fun Handling.createAllAsync(type: Any): Promise<List<Any>> {
    val creation = Creation(type, true).apply {
        wantsAsync = true
    }
    return try {
        handle(creation, true) failure {
            Promise.reject(NotHandledException(creation))
        } ?: (creation.result as Promise<*>) then { it as List<Any> }
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

suspend inline fun <reified T: Any> Handling.createAllCo() =
        createAllCo(typeOf<T>())

suspend fun Handling.createAllCo(type: Any) =
        with(coroutineContext).createAllAsync(type).await()