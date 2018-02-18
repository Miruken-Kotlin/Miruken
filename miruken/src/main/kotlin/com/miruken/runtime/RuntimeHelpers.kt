package com.miruken.runtime

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

val KType.isOpenGeneric: Boolean
    get() = classifier is KTypeParameter ||
                arguments.any { it.type?.isOpenGeneric == true }

inline fun <reified T> KAnnotatedElement.getTaggedAnnotations() =
        annotations.flatMap { it.annotationClass.annotations }
                   .filterIsInstance<T>()