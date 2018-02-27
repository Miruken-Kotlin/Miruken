package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.concurrent.Promise
import com.miruken.runtime.componentType
import com.miruken.runtime.isOpenGeneric
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.withNullability

sealed class TypeFlags(value: Long) : Flags<TypeFlags>(value) {
    object NONE       : TypeFlags(0)
    object LAZY       : TypeFlags(1 shl 0)
    object FUNC       : TypeFlags(1 shl 1)
    object ARRAY      : TypeFlags(1 shl 2)
    object COLLECTION : TypeFlags(1 shl 3)
    object PROMISE    : TypeFlags(1 shl 4)
    object OPTIONAL   : TypeFlags(1 shl 5)
    object PRIMITIVE  : TypeFlags(1 shl 6)
    object OPEN       : TypeFlags(1 shl 7)

    companion object {
        fun parse(type: KType) : Pair<Flags<TypeFlags>, KType> {
            var logicalType = type
            var flags = if (type.isOpenGeneric)
                -TypeFlags.OPEN else -TypeFlags.NONE

            logicalType = logicalType.takeIf { it.isMarkedNullable }?.let {
                flags += TypeFlags.OPTIONAL
                type.withNullability(false)
            } ?: logicalType

            logicalType = unwrapType(type, Lazy::class)?.let {
                flags += TypeFlags.LAZY; it }
                    ?: unwrapType(type, Function0::class)?.let {
                flags += TypeFlags.FUNC; it } ?: logicalType

            logicalType = unwrapType(logicalType, Promise::class)?.let {
                flags += TypeFlags.PROMISE; it } ?: logicalType

            logicalType = unwrapType(logicalType, Collection::class)?.let {
                flags += TypeFlags.COLLECTION; it } ?: logicalType

            if (!(flags has TypeFlags.COLLECTION)) {
                logicalType = logicalType.componentType.takeIf {
                    it != logicalType
                }?.also { flags += TypeFlags.ARRAY } ?: logicalType
            }

            (logicalType.classifier as? KClass<*>)?.also {
                if (it.javaPrimitiveType != null || it == String::class) {
                    flags += TypeFlags.PRIMITIVE
                }
            }

            return flags to logicalType
        }

        private fun unwrapType(type: KType, criteria: KClass<*>) : KType? {
            return (type.classifier as? KClass<*>)
                    ?.takeIf { it.isSubclassOf(criteria) }
                    ?.let { type.arguments[0].type}
        }
    }
}