package com.miruken.container

import com.miruken.callback.UseKeyResolver

@Target(AnnotationTarget.VALUE_PARAMETER,AnnotationTarget.PROPERTY)
@UseKeyResolver(ContainerKeyResolver::class)
annotation class Managed