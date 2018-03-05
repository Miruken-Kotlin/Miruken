package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseArgumentResolver<ProxyKeyResolver>(ProxyKeyResolver::class)
annotation class Proxy