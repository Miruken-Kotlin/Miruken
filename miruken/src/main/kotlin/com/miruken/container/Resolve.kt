package com.miruken.container

import com.miruken.callback.UseArgumentResolver

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseArgumentResolver<ContainerArgumentResolver>(ContainerArgumentResolver::class)
annotation class Resolve