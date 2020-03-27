package com.miruken.api

import com.miruken.callback.Handling
import com.miruken.callback.NotHandledException
import com.miruken.callback.handle
import com.miruken.concurrent.Promise
import com.miruken.kTypeOf
import kotlinx.coroutines.runBlocking

inline fun <reified T: Any> Handling.stashGet(): T? {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { return get.value as? T }
            ?: throw NotHandledException(get)
}

inline fun <reified T: Any> Handling.stashPut(data: T) {
    val put = StashAction.Put(kTypeOf<T>(), data)
    handle(put) failure {
        throw NotHandledException(put)
    }
}

inline fun <reified T: Any> Handling.stashDrop() {
    val drop = StashAction.Drop(kTypeOf<T>())
    handle(drop) failure {
        throw NotHandledException(drop)
    }
}

inline fun <reified T: Any> Handling.stashTryGet(): T? {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { get.value as? T }
}

inline fun <reified T: Any> Handling.stashGetOrPut(data: T): T? {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { get.value as? T }
            ?: data.also(::stashPut)
}

inline fun <reified T: Any> Handling.stashGetOrPut(data: () -> T): T? {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { get.value as? T }
            ?: data().also(::stashPut)
}

inline fun <reified T: Any> Handling.stashGetOrPutAsync(
        data: () -> Promise<T>
): T {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { get.value as? T }
            ?: data().get().also(::stashPut)
}

inline fun <reified T: Any> Handling.stashGetOrPutCo(
        crossinline data: suspend () -> T
): T {
    val get = StashAction.Get(kTypeOf<T>())
    return handle(get) success { get.value as? T }
            ?: runBlocking { data().also(::stashPut) }
}
