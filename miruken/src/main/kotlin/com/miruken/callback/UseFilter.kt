package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Repeatable annotation class UseFilter<F: Filtering<*,*>>(
        val filterClass: KClass<F>,
        val many:        Boolean = false,
        val order:       Int = -1)
