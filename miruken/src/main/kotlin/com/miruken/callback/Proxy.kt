package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseKeyResolver(ProxyKeyResolver::class)
annotation class Proxy