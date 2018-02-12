package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class In(val consumes: KClass<*>)

