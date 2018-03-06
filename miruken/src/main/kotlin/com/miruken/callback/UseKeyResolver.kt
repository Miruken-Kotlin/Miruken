package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseKeyResolver<R: KeyResolving>(
        val keyResolverClass: KClass<R>
)
