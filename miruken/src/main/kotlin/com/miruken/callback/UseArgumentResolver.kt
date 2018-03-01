package com.miruken.callback

import com.miruken.callback.policy.ArgumentResolving
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseArgumentResolver<R: ArgumentResolving>(
        val argumentResolverClass: KClass<R>
)
