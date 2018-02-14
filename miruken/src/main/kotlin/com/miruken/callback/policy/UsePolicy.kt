package com.miruken.callback.policy

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UsePolicy<P: CallbackPolicy>(val policyClass: KClass<P>)

fun getPolicyAnnotations(element:KAnnotatedElement) =
        element.annotations
            .flatMap { it.annotationClass.annotations }
            .filterIsInstance<UsePolicy<*>>()

val UsePolicy<*>.policy : CallbackPolicy?
    get() = policyClass.objectInstance