package com.miruken.callback.policy

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Policy<P: CallbackPolicy>(val policyClass: KClass<P>)

fun getPolicyAnnotations(element:KAnnotatedElement) =
        element.annotations
            .flatMap { it.annotationClass.annotations }
            .filterIsInstance<Policy<*>>()

val Policy<*>.policy : CallbackPolicy?
    get() = policyClass.objectInstance