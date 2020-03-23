package com.miruken.validate

import com.miruken.callback.UseFilter

@Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
@UseFilter(ValidateFilter::class)
annotation class Validate(val validateResponse: Boolean = false)