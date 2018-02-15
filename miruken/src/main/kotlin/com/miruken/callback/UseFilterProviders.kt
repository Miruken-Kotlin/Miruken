package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilterProviders<F: FilteringProvider>(
        vararg val filterProviderClasses: KClass<FilteringProvider>)