package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilterProviderFactory(
        val factoryClass: KClass<out FilteringProviderFactory>)
