package com.miruken

import kotlin.reflect.KType

inline fun <reified T : Any?> typeOf(): TypeReference =
        object : SuperTypeReference<T>() {}

inline fun <reified T : Any?> kTypeOf(): KType =
        object : SuperTypeReference<T>() {}.kotlinType



