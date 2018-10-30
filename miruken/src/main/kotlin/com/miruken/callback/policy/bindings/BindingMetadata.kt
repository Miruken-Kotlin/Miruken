package com.miruken.callback.policy.bindings

class BindingMetadata {
    private val _values = mutableMapOf<Any, Any?>()

    var name: String? = null

    fun isEmpty() = name == null && _values.isEmpty()

    fun has(key: Any) = _values.containsKey(key)

    @Suppress("UNCHECKED_CAST")
    fun <V> get(key: Any) = _values[key]?.let { it as V }

    fun set(key: Any, value: Any?) { _values[key] = value }

    fun mergeInto(other: BindingMetadata) {
        for (item in _values) {
            other.set(item.key, item.value)
        }
    }
}