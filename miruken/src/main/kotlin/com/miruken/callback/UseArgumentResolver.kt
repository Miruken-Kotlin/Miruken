package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseArgumentResolver<R: KeyResolving>(
        val argumentResolverClass: KClass<R>
)
