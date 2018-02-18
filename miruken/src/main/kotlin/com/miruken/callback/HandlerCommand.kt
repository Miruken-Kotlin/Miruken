package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.getKType
import kotlin.reflect.KType

inline fun <reified T: Any> Handling.command(callback: T) =
    command(callback, getKType<T>())

fun Handling.command(
        callback:     Any,
        callbackType: KType? = null
) : Any? {
    val command = Command(callback, callbackType)
    handle(command) failure {
        throw NotHandledException(
                "${callback::class.qualifiedName} not handled")
    }
    val result = command.result
    return when (result) {
        is Promise<*> -> result.get()
        else -> result
    }
}

inline fun <reified T: Any> Handling.commandAsync(callback: T) =
        commandAsync(callback, getKType<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAsync(
        callback:     Any,
        callbackType: KType? = null
): Promise<Any> {
    val command = Command(callback, callbackType).apply {
        wantsAsync = true
    }
    return handle(command) failure {
            Promise.reject(NotHandledException(
                    "${callback::class.qualifiedName} not handled"))
        } ?: command.result as Promise<Any>
    ?: Promise.Empty
}

inline fun <reified T: Any> Handling.commandAll(callback: T) =
        commandAll(callback, getKType<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAll(
        callback:     Any,
        callbackType: KType? = null
) : List<Any> {
    val command = Command(callback, callbackType, true)
    handle(command) failure {
        throw NotHandledException(
                "${callback::class.qualifiedName} not handled")
    }
    val result = command.result
    return when (result) {
        is Promise<*> -> result.get()
        else -> result
    } as List<Any>
}

inline fun <reified T: Any> Handling.commandAllAsync(callback: T) =
        commandAllAsync(callback, getKType<T>())

@Suppress("UNCHECKED_CAST")
fun Handling.commandAllAsync(
        callback:     Any,
        callbackType: KType? = null
): Promise<List<Any>> {
    val command = Command(callback, callbackType, true).apply {
        wantsAsync = true
    }
    return handle(command) failure {
        Promise.reject(NotHandledException(
                "${callback::class.qualifiedName} not handled"))
    } ?: (command.result as Promise<*>) then { it as List<Any> }
    ?: Promise.EmptyList
}