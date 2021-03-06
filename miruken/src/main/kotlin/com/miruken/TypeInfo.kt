package com.miruken

import com.miruken.callback.Inquiry
import com.miruken.callback.Strict
import com.miruken.concurrent.Promise
import com.miruken.runtime.checkOpenConformance
import com.miruken.runtime.componentType
import com.miruken.runtime.isGeneric
import com.miruken.runtime.isOpenGeneric
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.withNullability

data class TypeInfo(
        val type:          KType,
        val flags:         Flags<TypeFlags>,
        val logicalType:   KType,
        val componentType: KType
) {
    fun createInquiry(
            key:    Any,
            parent: Inquiry? = null
    ) = Inquiry(key, flags has TypeFlags.COLLECTION ||
            flags has TypeFlags.ARRAY, parent).apply {
        wantsAsync = flags has TypeFlags.PROMISE
    }

    fun mapOpenParameters(
            closedType:    KType,
            typeBindings:  MutableMap<KTypeParameter, KType>? = null
    ): MutableMap<KTypeParameter, KType>? {
        if (flags has TypeFlags.OPEN) {
            val bindings = typeBindings ?: mutableMapOf()
            componentType.checkOpenConformance(closedType, bindings, true)
            return bindings
        }
        return typeBindings
    }

    companion object {
        fun parse(
                type:      KType,
                annotated: KAnnotatedElement? = null
        ): TypeInfo {
            var logicalType = type

            var flags = when {
                type.isOpenGeneric -> TypeFlags.GENERIC + TypeFlags.OPEN
                type.isGeneric -> TypeFlags.GENERIC
                else -> -TypeFlags.NONE
            }

            (type.classifier as? KClass<*>)?.also {
                if (it.java.isInterface)
                    flags += TypeFlags.INTERFACE
            }

            if (type.annotations.filterIsInstance<Strict>().any() ||
                annotated?.annotations?.filterIsInstance<Strict>()?.any() == true) {
                flags += TypeFlags.STRICT
            }

            logicalType = logicalType.takeIf { it.isMarkedNullable }?.let {
                flags += TypeFlags.OPTIONAL
                type.withNullability(false)
            } ?: logicalType

            logicalType = unwrapType(logicalType, Optional::class)
                    ?.let { flags += TypeFlags.OPTIONAL_EXPLICIT; it }
                    ?: logicalType

            logicalType = unwrapType(type, Lazy::class)?.let {
                flags += TypeFlags.LAZY; it }
                    ?: unwrapType(type, Function0::class)?.let {
                flags += TypeFlags.FUNC; it } ?: logicalType

            logicalType = unwrapType(logicalType, Promise::class)
                    ?.let { flags += TypeFlags.PROMISE; it }
                    ?: logicalType

            val componentType = if (flags has TypeFlags.STRICT) {
                logicalType
            } else {
                unwrapType(logicalType, Collection::class)
                        ?.let { flags += TypeFlags.COLLECTION; it }
                        ?: logicalType.componentType.takeIf {
                            it != logicalType
                        }?.also { flags += TypeFlags.ARRAY }
                        ?: logicalType
            }

            (componentType.classifier as? KClass<*>)?.also {
                if (it.javaPrimitiveType != null || it == String::class)
                    flags += TypeFlags.PRIMITIVE
            }

            return TypeInfo(type, flags, logicalType, componentType)
        }

        private fun unwrapType(type: KType, criteria: KClass<*>): KType? {
            return (type.classifier as? KClass<*>)
                    ?.takeIf { it.isSubclassOf(criteria) }
                    ?.let { type.arguments[0].type}
        }
    }
}
