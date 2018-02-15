package com.miruken

import kotlin.reflect.full.declaredMemberProperties

open class Flags<T: Flags<T>> protected constructor() {
    var value: Long = 0
        private set

    @Suppress("UNUSED_PARAMETER")
    protected constructor(
            value: Long, unused: Boolean) : this() {
        this.value = value
    }

    operator fun unaryPlus() = value

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
            Flags(value or flag.value, true)

    infix operator fun minus(flag: Flags<T>): Flags<T> =
            Flags(value and flag.value.inv(), true)

    companion object {
        inline operator fun <reified T: Flags<T>> invoke(value : Long) =
                Flags<T>(value, true)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Flags<T>> valuesOf() : List<T> {
            val clazz = T::class
            return clazz.declaredMemberProperties
                    .filter {
                        val returnType = it.returnType
                        returnType.classifier == Flags::class &&
                        returnType.arguments.firstOrNull()?.let {
                            it.type?.classifier == clazz
                        } == true
                    }
                    .map { it.getter.call(clazz.objectInstance) } as List<T>
        }
    }
}
