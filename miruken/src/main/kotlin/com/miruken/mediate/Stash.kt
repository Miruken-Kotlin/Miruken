package com.miruken.mediate

import com.miruken.callback.Handler
import com.miruken.callback.notHandled
import com.miruken.typeOf

interface Stash {
    fun get(key: Any): Any?
    fun put(key: Any, data: Any)
    fun drop(key: Any): Boolean
}

inline fun <reified T: Any> Stash.get(): T? =
        get(typeOf<T>()) as? T

class StashImpl(
        private val root: Boolean = false
) : Handler(), Stash {
    private val _data = mutableMapOf<Any, Any>()

    override fun get(key: Any) =
            _data[key] ?: if (root) null else notHandled()

    override fun put(key: Any, data: Any) {
        _data[key] = data
    }

    override fun drop(key: Any) = _data.remove(key) != null
}