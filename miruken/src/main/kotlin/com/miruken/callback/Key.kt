package com.miruken.callback

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.VALUE_PARAMETER)
annotation class Key(val key:String, val caseSensitive: Boolean = false)