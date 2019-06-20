package com.miruken

@Suppress("ClassName")
sealed class TypeFlags(value: Long) : Flags<TypeFlags>(value) {
    object NONE              : TypeFlags(0)
    object LAZY              : TypeFlags(1 shl 0)
    object FUNC              : TypeFlags(1 shl 1)
    object COLLECTION        : TypeFlags(1 shl 2)
    object ARRAY             : TypeFlags(1 shl 3)
    object PROMISE           : TypeFlags(1 shl 4)
    object OPTIONAL          : TypeFlags(1 shl 5)
    object OPTIONAL_EXPLICIT : TypeFlags(1 shl 6)
    object PRIMITIVE         : TypeFlags(1 shl 7)
    object INTERFACE         : TypeFlags(1 shl 8)
    object STRICT            : TypeFlags(1 shl 9)
    object GENERIC           : TypeFlags(1 shl 10)
    object OPEN              : TypeFlags(1 shl 11)
}