package com.miruken.map

object EqualityMatcher : FormatMatching {
    override fun matches(
            annotation: Annotation,
            format: Any
    ) = (annotation as? Format)?.format?.equals(format) == true
}