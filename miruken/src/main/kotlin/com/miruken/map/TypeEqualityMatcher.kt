package com.miruken.map

object TypeEqualityMatcher : FormatMatching {
    override fun matches(
            annotation: Annotation,
            format:     Any
    ) = (annotation as? FormatType)?.format?.equals(format) == true
}