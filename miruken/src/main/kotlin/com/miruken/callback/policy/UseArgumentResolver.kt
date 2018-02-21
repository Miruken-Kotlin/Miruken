package com.miruken.callback.policy

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseArgumentResolver<R: ArgumentResolving>(
        val argumentResolverClass: KClass<R>
)
