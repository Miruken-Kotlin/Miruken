package com.miruken.map

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFormatMatcher<R: FormatMatching>(
        val formatMatcherClass: KClass<R>
)
