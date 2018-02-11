package com.miruken.callback

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Key(val keyClass: KClass<*>)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class StringKey(
        val keyString: String, val caseInsensitive:Boolean = false)
