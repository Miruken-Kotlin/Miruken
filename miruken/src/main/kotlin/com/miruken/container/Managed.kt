package com.miruken.container

import com.miruken.callback.UseArgumentResolver

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseArgumentResolver<ContainerKeyResolver>(ContainerKeyResolver::class)
annotation class Managed