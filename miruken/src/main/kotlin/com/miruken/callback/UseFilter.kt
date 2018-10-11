package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilter(
        val filterClass: KClass<out Filtering<*,*>>,
        val many:        Boolean = false,
        val order:       Int     = -1,
        val required:    Boolean = false)

