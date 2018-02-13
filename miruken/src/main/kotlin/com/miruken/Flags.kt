package com.miruken

abstract class Flags<T: Flags<T>>(val value: Long) {

    val name: String by lazy {
        with (this::class) {
            objectInstance?.let { simpleName }
                    ?: value.toString()
        }
    }

    operator fun unaryPlus() = value

    infix fun hasFlag(flag: T): Boolean {
        if (value == 0L || (value > 0L && flag.value == 0L))
            return false
        return value and flag.value == flag.value
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean =
            (other as? T)?.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String =
            "${this::class.simpleName} ($value)"

    protected abstract fun coerceValue(value: Long) : T

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal inline fun <reified T: Flags<T>> fromValue(value: Long) : T {
        return flagValues<T>()
                .firstOrNull { it.value == value }
                ?: internalCoerce(value) as T
    }

    @PublishedApi
    internal fun internalCoerce(value: Long) = coerceValue(value)
}

inline infix operator fun <reified T: Flags<T>> T.plus(flag: T): T =
        fromValue(value or flag.value)

inline infix operator fun <reified T: Flags<T>> T.minus(flag: T): T =
        fromValue(value and flag.value.inv())

inline fun <reified T: Flags<T>> T.set(flag: T, condition: Boolean = true) =
        if (condition) this + flag else this

inline fun <reified T: Flags<T>> T.unset(flag: T, condition: Boolean = true) =
        if (condition) this - flag else this

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Flags<T>> flagValues() : List<T> {
    return T::class.nestedClasses.mapNotNull {
        it.objectInstance } as List<T>
}