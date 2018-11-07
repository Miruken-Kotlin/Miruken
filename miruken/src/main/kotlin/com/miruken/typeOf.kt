package com.miruken

import kotlin.reflect.KType

inline fun <reified T : Any?> typeOf(): KType =
        object : TypeReference<T>() {}.kotlinType




