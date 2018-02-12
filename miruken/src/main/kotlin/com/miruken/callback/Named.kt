package com.miruken.callback

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Named(
        val name: String,
        val caseInsensitive:Boolean = true
)
