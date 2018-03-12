package com.miruken.callback.policy

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UsePolicy(val policyClass: KClass<out CallbackPolicy>)

val UsePolicy.policy : CallbackPolicy?
    get() = policyClass.objectInstance