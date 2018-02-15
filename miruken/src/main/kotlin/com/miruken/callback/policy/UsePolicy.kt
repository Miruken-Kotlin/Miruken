package com.miruken.callback.policy

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UsePolicy<P: CallbackPolicy>(val policyClass: KClass<P>)

val UsePolicy<*>.policy : CallbackPolicy?
    get() = policyClass.objectInstance