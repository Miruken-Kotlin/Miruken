package com.miruken.mediate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.typeOf
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> Handling.send(request: T) =
        send(request, typeOf<T>())

fun Handling.send(
        request:     Any,
        requestType: KType
): Promise<*> {
    val command = Command(request, requestType).apply {
        wantsAsync = true
    }
    return try {
        (StashImpl() + infer).handle(command) failure {
            Promise.reject(NotHandledException(request))
        } ?: command.result as Promise<*>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <TResp: Any, reified T: Request<TResp>>
        Handling.send(request: T): Promise<TResp> =
        send(request, typeOf<T>())

fun <TResp: Any> Handling.send(
        request:     Request<TResp>,
        requestType: KType
): Promise<TResp> {
    val command = Command(request, requestType).apply {
        wantsAsync = true
    }
    return try {
        @Suppress("UNCHECKED_CAST")
        (StashImpl() + infer).handle(command) failure {
            Promise.reject(NotHandledException(request))
        } ?: command.result as Promise<TResp>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

fun Handling.publish(
        notification:     Any,
        notificationType: KType
): Promise<*> {
    val command = Command(notification, notificationType, true).apply {
        wantsAsync = true
    }
    return try {
        (StashImpl() + infer).handle(command) failure {
            Promise.reject(NotHandledException(notification))
        } ?: command.result as Promise<*>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}