package com.miruken.callback

import com.miruken.concurrent.Promise

fun Handling.command(callback: Any) : Any? {
    val command = Command(callback)
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
fun Handling.commandAsync(callback: Any): Promise<Any> {
    val command = Command(callback).apply { wantsAsync = true }
    return handle(command) failure {
            Promise.reject(NotHandledException(
                    "${callback::class.qualifiedName} not handled"))
        } ?: command.result as Promise<Any>
    ?: Promise.Empty
}

@Suppress("UNCHECKED_CAST")
fun Handling.commandAll(callback: Any) : List<Any> {
    val command = Command(callback, true)
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
fun Handling.commandAllAsync(callback: Any): Promise<List<Any>> {
    val command = Command(callback).apply { wantsAsync = true }
    return handle(command) failure {
        Promise.reject(NotHandledException(
                "${callback::class.qualifiedName} not handled"))
    } ?: (command.result as Promise<*>) then { it as List<Any> }
    ?: Promise.EmptyList
}