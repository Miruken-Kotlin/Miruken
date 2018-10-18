package com.miruken.mediate

import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import com.miruken.protocol.proxy
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
class StashOf<T: Any>(
        val key: KType,
        handler: Handling) {
    val stash = handler.proxy<Stash>()

    var value: T?
        get() = try {
            stash.get(key) as? T
        } catch (e: Throwable) {
            null
        }
        set(value) {
            value?.also { stash.put(key, it) }
        }
}

inline fun <reified T: Any> StashOf<T>.getOrPut(data: T) =
        stash.getOrPut(data)

inline fun <reified T: Any> StashOf<T>.getOrPut(data: () -> T) =
        stash.getOrPut(data)

inline fun <reified T: Any> StashOf<T>.getOrPutAsync(
        data: () -> Promise<T>
) = stash.getOrPutAsync(data)

inline fun <reified T: Any> StashOf<T>.drop() = stash.drop<T>()