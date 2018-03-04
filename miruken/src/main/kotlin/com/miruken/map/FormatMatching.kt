package com.miruken.map

interface FormatMatching {
    fun matches(annotation: Annotation, format: Any): Boolean
}