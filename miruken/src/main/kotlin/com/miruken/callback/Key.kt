package com.miruken.callback

@Target(AnnotationTarget.FUNCTION)
annotation class Key(val key:String, val caseSensitive: Boolean = false)