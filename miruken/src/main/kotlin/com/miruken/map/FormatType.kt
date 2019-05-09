package com.miruken.map

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@UseFormatMatcher<TypeEqualityMatcher>(TypeEqualityMatcher::class)
annotation class FormatType(val format: KClass<*>)