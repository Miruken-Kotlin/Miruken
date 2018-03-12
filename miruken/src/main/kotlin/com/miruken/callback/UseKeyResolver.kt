package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseKeyResolver(
        val keyResolverClass: KClass<out KeyResolving>
)
