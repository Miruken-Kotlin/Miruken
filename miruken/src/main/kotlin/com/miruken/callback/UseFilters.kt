package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilters<F: Filtering<*,*>>(
        vararg val filterClasses: KClass<Filtering<*,*>>)
