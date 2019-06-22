package com.miruken.api

import com.miruken.callback.*
import kotlin.collections.set

sealed class StashAction {
    data class Get(val key: Any, var value: Any? = null): StashAction()
    data class Put(val key: Any, var value: Any? = null): StashAction()
    data class Drop(val key: Any): StashAction()
}

class StashImpl(
        private val root: Boolean = false
) : Handler() {
    private val _data = mutableMapOf<Any, Any?>()

    @Provides @Strict
    fun provide(inquiry: Inquiry) = _data[inquiry.key]

    @Handles
    fun get(get: StashAction.Get): Any? {
        if (_data.containsKey(get.key)) {
            get.value = _data[get.key]
            return true
        }
        return if (root) true else null
    }

    @Handles
    fun put(put: StashAction.Put) {
        _data[put.key] = put.value
    }

    @Handles
    fun drop(drop: StashAction.Drop) {
        _data.remove(drop.key)
    }
}
