package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER,AnnotationTarget.PROPERTY_GETTER)
@UseKeyResolver(ProxyKeyResolver::class)
annotation class Proxy