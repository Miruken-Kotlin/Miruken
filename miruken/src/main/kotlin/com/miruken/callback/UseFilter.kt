package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
@Repeatable annotation class UseFilter(
        val filterClass: KClass<out Filtering<*,*>>,
        val many:        Boolean = false,
        val order:       Int = -1)
