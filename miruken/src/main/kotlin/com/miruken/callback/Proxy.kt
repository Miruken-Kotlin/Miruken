package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER,AnnotationTarget.PROPERTY)
@UseKeyResolver(ProxyKeyResolver::class)
annotation class Proxy