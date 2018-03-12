package com.miruken.container

import com.miruken.callback.UseKeyResolver

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseKeyResolver(ContainerKeyResolver::class)
annotation class Managed