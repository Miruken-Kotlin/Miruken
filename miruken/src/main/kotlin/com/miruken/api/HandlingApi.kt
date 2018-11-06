package com.miruken.api

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.typeOf
import kotlin.reflect.KType

inline fun <reified T: NamedType> Handling.send(request: T) =
        send(request, typeOf<T>())

fun Handling.send(
        request:     NamedType,
        requestType: KType?
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

inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Handling.send(request: T): Promise<TResp> =
        send(request, typeOf<T>())

fun <TResp: NamedType?> Handling.send(
        request:     Request<TResp>,
        requestType: KType?
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

inline fun <reified T: NamedType> Handling.publish(notification: T) =
        publish(notification, typeOf<T>())

fun Handling.publish(
        notification:     NamedType,
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
