package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class Out(val provides: KClass<*>)

