package com.miruken.callback.policy.bindings

import com.miruken.callback.FilteringProviderFactory
import com.miruken.callback.UseFilterProviderFactory

class MetadataKeyConstraint(
        private val key:   Any,
        private val value: Any?
): BindingConstraint {

    override fun require(metadata: BindingMetadata) =
            metadata.set(key, value)

    override fun matches(metadata: BindingMetadata) =
            metadata.has(key) && metadata.get<Any>(key) == value
}

class MetadataConstraint(
        private val metadata: Map<Any, Any?>
) : BindingConstraint {

    override fun require(metadata: BindingMetadata) {
        for ((key, value) in this.metadata) {
            metadata.set(key, value)
        }
    }

    override fun matches(metadata: BindingMetadata): Boolean {
        for ((key, value) in this.metadata) {
            if (!metadata.has(key)) return false
            return metadata.get<Any>(key) == value
        }
        return true
    }
}

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class Metadata(val key: String)

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class MetadataBool(val key: String, val value: Boolean)

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class MetadataInt(val key: String, val value: Int)

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class MetadataLong(val key: String, val value: Long)

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class MetadataFloat(val key: String, val value: Float)

@UseFilterProviderFactory(MetadataConstraintFactory::class)
annotation class MetadataDouble(val key: String, val value: Double)

object MetadataConstraintFactory : FilteringProviderFactory {
    override fun createProvider(annotation: Annotation) =
        ConstraintProvider(when (annotation) {
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
        })
}