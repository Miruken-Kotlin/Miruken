package com.miruken.api

import com.miruken.TypeReference
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.context.Contextual
import com.miruken.context.requireContext
import com.miruken.typeOf

inline fun <reified T: NamedType> Handling.send(request: T) =
        send(request, typeOf<T>())

inline fun <reified T: NamedType> Contextual.send(request: T) =
        requireContext().send(request)

fun Handling.send(
        request:     NamedType,
        requestType: TypeReference?
): Promise<*> {
    val command = Command(request, requestType).apply {
        wantsAsync = true
    }
    return try {
        (StashImpl() + this).handle(command) failure {
            Promise.reject(NotHandledException(request))
        } ?: command.result as Promise<*>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

fun Contextual.send(
        request:     NamedType,
        requestType: TypeReference?
) = requireContext().send(request, requestType)

inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Handling.send(request: T): Promise<TResp> =
        send(request, typeOf<T>())

inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Contextual.send(request: T) = requireContext().send(request)

fun <TResp: NamedType?> Handling.send(
        request:     Request<TResp>,
        requestType: TypeReference?
): Promise<TResp> {
    val command = Command(request, requestType).apply {
        wantsAsync = true
    }
    return try {
        @Suppress("UNCHECKED_CAST")
        (StashImpl() + this).handle(command) failure {
            Promise.reject(NotHandledException(request))
        } ?: command.result as Promise<TResp>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

fun <TResp: NamedType?> Contextual.send(
        request:     Request<TResp>,
        requestType: TypeReference?
) = requireContext().send(request, requestType)

inline fun <reified T: NamedType> Handling.publish(notification: T) =
        publish(notification, typeOf<T>())

inline fun <reified T: NamedType> Contextual.publish(notification: T) =
        requireContext().publish(notification)

fun Handling.publish(
        notification:     NamedType,
        notificationType: TypeReference?
): Promise<*> {
    val command = Command(notification, notificationType, true).apply {
        wantsAsync = true
    }
    return try {
        (StashImpl() + this).handle(command) failure {
            Promise.reject(NotHandledException(notification))
        } ?: command.result as Promise<*>
    } catch (e: Throwable) {
        Promise.reject(e)
    }
}

fun Contextual.publish(
        notification:     NamedType,
        notificationType: TypeReference?
) = requireContext().publish(notification, notificationType)