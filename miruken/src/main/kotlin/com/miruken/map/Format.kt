package com.miruken.map

@Target(AnnotationTarget.FUNCTION)
@UseFormatMatcher<EqualityMatcher>(EqualityMatcher::class)
annotation class Format(val format: String)