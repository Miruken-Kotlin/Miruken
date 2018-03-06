package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER)
@UseKeyResolver<ProxyKeyResolver>(ProxyKeyResolver::class)
annotation class Proxy