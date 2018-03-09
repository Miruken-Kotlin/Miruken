package com.miruken

import kotlin.reflect.KType

data class TypeInfo(
        val type:          KType,
        val flags:         Flags<TypeFlags>,
        val logicalType:   KType,
        val componentType: KType)