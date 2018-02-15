package com.miruken

open class Flags<T: Flags<T>>(val value: Long) {
    val name: String by lazy {
        with(this::class) {
            objectInstance?.let { simpleName }
                    ?: value.toString()
        }
    }

    operator fun unaryPlus() = value

    operator fun unaryMinus() : Flags<T> = toFlag()

    fun toFlag() : Flags<T> = this as? Flags<T> ?: Flags(value)

    infix fun hasFlag(flag: Flags<T>): Boolean {
        if (value == 0L || (value > 0L && flag.value == 0L))
            return false
        return value and flag.value == flag.value
    }

    fun setFlag(flag: Flags<T>, enabled: Boolean = true,
            condition: (() -> Boolean)? = null
    ): Flags<T> {
        return if (condition == null || condition()) {
            if (enabled) this + flag else this - flag
        } else this
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean =
            (other as? Flags<T>)?.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String =
            "${this::class.simpleName} ($value)"

    infix operator fun plus(flag: Flags<T>): Flags<T> =
            Flags(value or flag.value)

    infix operator fun minus(flag: Flags<T>): Flags<T> =
            Flags(value and flag.value.inv())

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Flags<T>> valuesOf() : List<T> {
            return T::class.nestedClasses.mapNotNull {
                it.objectInstance } as List<T>
        }
    }
}

