package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.typeOf
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.command(callback: T) =
    command(callback, typeOf<T>())

fun Handling.command(
        callback:     Any,
        callbackType: KType
): Any? {
    val command = Command(callback, callbackType)
    handle(command) failure {
        throw NotHandledException(callback)
    }
    return command.result
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.commandAsync(callback: T) =
        commandAsync(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAsync(
        callback:     Any,
        callbackType: KType
): Promise<Any> {
    val command = Command(callback, callbackType).apply {
        wantsAsync = true
    }
    return try {
        handle(command) failure {
            Promise.reject(NotHandledException(callback))
        } ?: command.result as Promise<Any>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.commandAll(callback: T) =
        commandAll(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAll(
        callback:     Any,
        callbackType: KType
): List<Any> {
    val command = Command(callback, callbackType, true)
    handle(command, true) failure {
        throw NotHandledException(callback)
    }
    return command.result as List<Any>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.commandAllAsync(callback: T) =
        commandAllAsync(callback, typeOf<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAllAsync(
        callback:     Any,
        callbackType: KType
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