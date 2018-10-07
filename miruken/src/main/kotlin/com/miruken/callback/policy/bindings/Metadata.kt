package com.miruken.callback.policy.bindings

import com.miruken.callback.FilteringProvider
import com.miruken.callback.FilteringProviderFactory

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class Metadata(val key: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class MetadataBool(val key: String, val value: Boolean)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class MetadataInt(val key: String, val value: Int)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class MetadataLong(val key: String, val value: Long)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class MetadataFloat(val key: String, val value: Float)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class MetadataDouble(val key: String, val value: Double)

class MetadataConstraintFactory : FilteringProviderFactory
{
    override fun createProvider(
            annotation: Annotation
    ): FilteringProvider {
        val constraint = when (annotation) {
            is Metadata ->
                MetadataKeyConstraint(annotation.key, null)
            is MetadataBool ->
                MetadataKeyConstraint(annotation.key, annotation.value)
            is MetadataInt ->
                MetadataKeyConstraint(annotation.key, annotation.value)
            is MetadataFloat ->
                MetadataKeyConstraint(annotation.key, annotation.value)
            is MetadataDouble ->
                MetadataKeyConstraint(annotation.key, annotation.value)
            else -> error("Unrecognized metadata '$annotation'")
        }
        return ConstraintProvider(constraint)
    }
}