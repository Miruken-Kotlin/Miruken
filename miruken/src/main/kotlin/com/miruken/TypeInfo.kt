package com.miruken

import com.miruken.runtime.mapOpenParameters
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

data class TypeInfo(
        val type:          KType,
        val flags:         Flags<TypeFlags>,
        val logicalType:   KType,
        val componentType: KType)

fun TypeInfo.mapOpenParameters(
        closedType:    KType,
        typeBindings:  MutableMap<KTypeParameter, KType>? = null
): MutableMap<KTypeParameter, KType>? {
    if (flags has TypeFlags.OPEN) {
        val bindings = typeBindings ?: mutableMapOf()
        componentType.mapOpenParameters(closedType, bindings, true)
        return bindings
    }
    return typeBindings
}