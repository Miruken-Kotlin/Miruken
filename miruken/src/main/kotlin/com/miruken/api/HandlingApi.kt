package com.miruken.api

import com.miruken.TypeReference
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.context.Contextual
import com.miruken.context.requireContext
import com.miruken.typeOf
import kotlin.coroutines.coroutineContext

inline fun <reified T: NamedType> Handling.send(request: T) =
        send(request, typeOf<T>())

suspend inline fun <reified T: NamedType> Handling.sendCo(request: T) =
        with(coroutineContext)
                .send(request, typeOf<T>())
                .await()

inline fun <reified T: NamedType> Contextual.send(request: T) =
        requireContext().send(request)

suspend inline fun <reified T: NamedType> Contextual.sendCo(request: T) =
        requireContext().with(coroutineContext)
                .send(request, typeOf<T>())
                .await()

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

suspend fun Handling.sendCo(
        request:     NamedType,
        requestType: TypeReference?
) = with(coroutineContext)
        .send(request, requestType).
        await()

fun Contextual.send(
        request:     NamedType,
        requestType: TypeReference?
) = requireContext().send(request, requestType)

suspend fun Contextual.sendCo(
        request:     NamedType,
        requestType: TypeReference?
) = requireContext().with(coroutineContext)
        .send(request, requestType)
        .await()

inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Handling.send(request: T): Promise<TResp> =
        send(request, typeOf<T>())

suspend inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Handling.sendCo(request: T) = with(coroutineContext)
        .send(request, typeOf<T>())
        .await()

inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Contextual.send(request: T) = requireContext().send(request)

suspend inline fun <TResp: NamedType?, reified T: Request<TResp>>
        Contextual.sendCo(request: T) = requireContext()
        .with(coroutineContext)
        .send(request)
        .await()

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

suspend fun <TResp: NamedType?> Handling.sendCo(
        request:     Request<TResp>,
        requestType: TypeReference?
) = with(coroutineContext)
        .send(request, requestType)
        .await()

fun <TResp: NamedType?> Contextual.send(
        request:     Request<TResp>,
        requestType: TypeReference?
) = requireContext().send(request, requestType)

suspend fun <TResp: NamedType?> Contextual.sendCo(
        request:     Request<TResp>,
        requestType: TypeReference?
) = requireContext().with(coroutineContext)
        .send(request, requestType)
        .await()

inline fun <reified T: NamedType> Handling.publish(notification: T) =
        publish(notification, typeOf<T>())

suspend inline fun <reified T: NamedType> Handling.publishCo(notification: T) =
        with(coroutineContext)
                .publish(notification, typeOf<T>())
                .await()

inline fun <reified T: NamedType> Contextual.publish(notification: T) =
        requireContext().publish(notification)

suspend inline fun <reified T: NamedType> Contextual.publishCo(notification: T) =
        requireContext().with(coroutineContext)
                .publish(notification)
                .await()

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

suspend fun Handling.publishCo(
        notification:     NamedType,
        notificationType: TypeReference?
) = with(coroutineContext)
        .publish(notification, notificationType)
        .await()

fun Contextual.publish(
        notification:     NamedType,
        notificationType: TypeReference?
) = requireContext().publish(notification, notificationType)

suspend fun Contextual.publishCo(
        notification:     NamedType,
        notificationType: TypeReference?
) = requireContext().with(coroutineContext)
        .publish(notification, notificationType)
        .await()