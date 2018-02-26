package com.miruken.callback

class StringKey(val key: String, val caseSensitive: Boolean = false) {
    override fun equals(other: Any?): Boolean {
        return (other === this) || ((other as? StringKey)?.key)
                ?.let { it == key || (!caseSensitive &&
                    String.CASE_INSENSITIVE_ORDER.compare(key, it) == 0)
                } ?: false
    }

    override fun hashCode(): Int = key.hashCode()
}