package com.miruken.api

import com.miruken.callback.Handler
import com.miruken.callback.Inquiry
import com.miruken.callback.Provides
import com.miruken.callback.notHandled
import com.miruken.concurrent.Promise
import com.miruken.kTypeOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

interface Stash {
    fun get(key: Any): Any?
    fun put(key: Any, data: Any)
    fun drop(key: Any): Boolean
}

class StashImpl(
        private val root: Boolean = false
) : Handler(), Stash {
    private val _data = mutableMapOf<Any, Any>()

    @Provides
    fun provide(inquiry: Inquiry) = _data[inquiry.key]

    override fun get(key: Any) =
            _data[key] ?: if (root) null else notHandled()

    override fun put(key: Any, data: Any) {
        _data[key] = data
    }

    override fun drop(key: Any) = _data.remove(key) != null
}

inline fun <reified T: Any> Stash.get(): T? =
        get(kTypeOf<T>()) as? T

inline fun <reified T: Any> Stash.put(data: T) =
        put(kTypeOf<T>(), data)

inline fun <reified T: Any> Stash.drop() =
        drop(kTypeOf<T>())

inline fun <reified T: Any> Stash.tryGet(): T? =
        try {
            get()
        } catch (e: Throwable) {
            null
        }

inline fun <reified T: Any> Stash.getOrPut(data: T) =
        tryGet() ?: data.also { put(it) }

inline fun <reified T: Any> Stash.getOrPut(data: () -> T) =
        tryGet() ?: data().also { put(it) }

inline fun <reified T: Any> Stash.getOrPutAsync(
        data: () -> Promise<T>
) = tryGet() ?: data().also { put(it.get()) }
