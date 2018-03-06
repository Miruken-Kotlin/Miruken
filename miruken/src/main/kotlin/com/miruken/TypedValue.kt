package com.miruken

import kotlin.reflect.KType

interface TypedValue {
    val type:  KType
    val value: Any
}