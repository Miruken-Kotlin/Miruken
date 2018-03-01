package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseArgumentResolver<ProxyArgumentResolver>(ProxyArgumentResolver::class)
annotation class Proxy