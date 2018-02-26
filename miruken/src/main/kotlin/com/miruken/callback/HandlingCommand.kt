package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
inline fun <reified R: Any> Handling.command(callback: Any) =
    command(callback, typeOf<R>()) as R

fun Handling.command(
        callback:   Any,
        resultType: KType? = null
) : Any? {
    val command = Command(callback, resultType)
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

@Suppress("UNCHECKED_CAST")
inline fun <reified R: Any> Handling.commandAsync(callback: Any) =
        commandAsync(callback, typeOf<R>()) as Promise<R>

@Suppress("UNCHECKED_CAST")
fun Handling.commandAsync(
        callback:   Any,
        resultType: KType? = null
): Promise<Any> {
    val command = Command(callback, resultType).apply {
        wantsAsync = true
    }
    return handle(command) failure {
            Promise.reject(NotHandledException(
                    "${callback::class.qualifiedName} not handled"))
    } ?: command.result as Promise<Any>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified R: Any> Handling.commandAll(callback: Any) =
        commandAll(callback, typeOf<R>()) as List<R>

@Suppress("UNCHECKED_CAST")
fun Handling.commandAll(
        callback:   Any,
        resultType: KType? = null
) : List<Any> {
    val command = Command(callback, resultType, true)
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

@Suppress("UNCHECKED_CAST")
inline fun <reified R: Any> Handling.commandAllAsync(callback: Any) =
        commandAllAsync(callback, typeOf<R>()) as Promise<List<R>>

@Suppress("UNCHECKED_CAST")
fun Handling.commandAllAsync(
        callback:   Any,
        resultType: KType? = null
): Promise<List<Any>> {
    val command = Command(callback, resultType, true).apply {
        wantsAsync = true
    }
    return handle(command) failure {
        Promise.reject(NotHandledException(
                "${callback::class.qualifiedName} not handled"))
    } ?: (command.result as Promise<*>) then { it as List<Any> }
}