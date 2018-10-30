package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilterProvider(
        vararg val provideBy: KClass<out FilteringProvider>)
