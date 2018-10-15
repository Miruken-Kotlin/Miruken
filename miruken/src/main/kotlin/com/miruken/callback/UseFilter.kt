package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilter(
        val filterBy: KClass<out Filtering<*,*>>,
        val order:    Int     = -1,
        val required: Boolean = false)

