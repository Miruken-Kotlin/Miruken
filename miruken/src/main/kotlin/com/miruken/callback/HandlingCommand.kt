package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.typeOf
import kotlin.coroutines.coroutineContext

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.command(callback: T) =
    command(callback, typeOf<T>())

fun Handling.command(
        callback:     Any,
        callbackType: TypeReference
): Any? {
    val command = Command(callback, callbackType)
    handle(command) failure {
        throw NotHandledException(callback)
    }
    return command.result
}

inline fun <reified T: Any> Handling.commandAsync(callback: T) =
        commandAsync(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAsync(
        callback:     Any,
        callbackType: TypeReference
): Promise<Any?> {
    val command = Command(callback, callbackType).apply {
        wantsAsync = true
    }
    return try {
        handle(command) failure {
            Promise.reject(NotHandledException(callback))
        } ?: command.result as Promise<Any?>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

suspend inline fun <reified T: Any> Handling.commandCo(callback: T) =
        with(coroutineContext)
                .commandAsync(callback)
                .await()

suspend fun Handling.commandCo(
        callback:     Any,
        callbackType: TypeReference
) = with(coroutineContext)
        .commandAsync(callback, callbackType).await()

inline fun <reified T: Any> Handling.commandAll(callback: T) =
        commandAll(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAll(
        callback:     Any,
        callbackType: TypeReference
): List<Any> {
    val command = Command(callback, callbackType, true)
    handle(command, true) failure {
        throw NotHandledException(callback)
    }
    return command.result as List<Any>
}

inline fun <reified T: Any> Handling.commandAllAsync(callback: T) =
        commandAllAsync(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAllAsync(
        callback:     Any,
        callbackType: TypeReference
): Promise<List<Any>> {
    val command = Command(callback, callbackType, true).apply {
        wantsAsync = true
    }
    return try {
        handle(command, true) failure {
            Promise.reject(NotHandledException(callback))
        } ?: (command.result as Promise<*>) then { it as List<Any> }
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

suspend inline fun <reified T: Any> Handling.commandAllCo(callback: T) =
        with(coroutineContext)
                .commandAllAsync(callback)
                .await()

suspend fun Handling.commandAllCo(
        callback:     Any,
        callbackType: TypeReference
) = with(coroutineContext)
        .commandAllAsync(callback, callbackType)
        .await()