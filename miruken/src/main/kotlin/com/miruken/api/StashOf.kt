package com.miruken.api

import com.miruken.callback.Handling
import com.miruken.callback.handle
import com.miruken.concurrent.Promise
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
class StashOf<T: Any>(val key: KType, val handler: Handling) {
    var value: T?
        get() {
            val get = StashAction.Get(key)
            return handler.handle(get) success { get.value as? T }
        }
        set(value) {
            val put = StashAction.Put(key, value)
            handler.handle(put)
        }
}

inline fun <reified T: Any> StashOf<T>.getOrPut(data: T) =
        handler.stashGetOrPut(data)

inline fun <reified T: Any> StashOf<T>.getOrPut(data: () -> T) =
        handler.stashGetOrPut(data)

inline fun <reified T: Any> StashOf<T>.getOrPutAsync(
        data: () -> Promise<T>
) = handler.stashGetOrPutAsync(data)

inline fun <reified T: Any> StashOf<T>.drop() = handler.stashDrop<T>()