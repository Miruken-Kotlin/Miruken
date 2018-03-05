package com.miruken

import kotlin.reflect.KType

interface TypedValue {
    val value: Any
    val type:  KType
}