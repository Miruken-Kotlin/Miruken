package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.concurrent.Promise
import com.miruken.runtime.isOpenGeneric
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.withNullability

sealed class TypeFlags(value: Long) : Flags<TypeFlags>(value) {
    object NONE       : TypeFlags(0)
    object LAZY       : TypeFlags(1 shl 0)
    object COLLECTION : TypeFlags(1 shl 1)
    object PROMISE    : TypeFlags(1 shl 2)
    object OPTIONAL   : TypeFlags(1 shl 3)
    object PRIMITIVE  : TypeFlags(1 shl 4)
    object OPEN       : TypeFlags(1 shl 5)

    companion object {
        fun parse(type: KType) : Pair<Flags<TypeFlags>, KType> {
            var logicalType = type
            var flags = if (type.isOpenGeneric)
                -TypeFlags.OPEN else -TypeFlags.NONE

            logicalType = logicalType.takeIf { it.isMarkedNullable }?.let {
                flags += TypeFlags.OPTIONAL
                type.withNullability(false)
            } ?: logicalType

            logicalType = extractType(type, Lazy::class)?.let {
                flags += TypeFlags.LAZY; it }
                    ?: extractType(type, Function0::class)?.let {
                flags += TypeFlags.LAZY; it } ?: logicalType

            logicalType = extractType(type, Promise::class)?.let {
                flags += TypeFlags.PROMISE; it } ?: logicalType

            logicalType = extractType(type, Collection::class)?.let {
                flags += TypeFlags.COLLECTION; it } ?: logicalType

            logicalType = logicalType.takeIf { it.arguments.isEmpty() }
                    ?.let { it.classifier as? KTypeParameter }
                    ?.upperBounds?.firstOrNull()?.withNullability(false)
                    ?: logicalType

            (logicalType.classifier as? KClass<*>)?.also {
                if (it.javaPrimitiveType != null || it == String::class) {
                    flags += TypeFlags.PRIMITIVE
                }
            }

            return flags to logicalType
        }

        private fun extractType(type: KType, criteria: KClass<*>) : KType? {
            return (type.classifier as? KClass<*>)
                    ?.takeIf { it.isSubclassOf(criteria) }
                    ?.let { type.arguments[0].type}
        }
    }
}