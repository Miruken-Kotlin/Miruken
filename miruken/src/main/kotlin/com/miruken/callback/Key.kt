package com.miruken.callback

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER)
annotation class Key(val key:String, val caseSensitive: Boolean = false)