package com.miruken

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

val KType.isOpenGeneric : Boolean get() =
    classifier is KTypeParameter ||
    arguments.any { it.type?.isOpenGeneric == true}