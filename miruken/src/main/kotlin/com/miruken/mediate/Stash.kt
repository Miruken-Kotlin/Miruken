package com.miruken.mediate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.protocol.proxy
import com.miruken.typeOf
import kotlin.reflect.KType

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
        get(typeOf<T>()) as? T

inline fun <reified T: Any> Stash.put(data: T) =
        put(typeOf<T>(), data)

inline fun <reified T: Any> Stash.drop() =
        drop(typeOf<T>())

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
