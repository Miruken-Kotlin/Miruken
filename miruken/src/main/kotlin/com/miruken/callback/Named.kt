package com.miruken.callback

import com.miruken.callback.policy.bindings.BindingConstraint
import com.miruken.callback.policy.bindings.BindingMetadata
import com.miruken.callback.policy.bindings.ConstraintProvider

class NamedConstraint(val name: String) : BindingConstraint {

    override fun require(metadata: BindingMetadata) {
        metadata.name = name
    }

    override fun matches(metadata: BindingMetadata) =
            metadata.name == null || metadata.name == name
}

object NamedConstraintFactory : FilteringProviderFactory {
    override fun createProvider(
            annotation: Annotation
    ): FilteringProvider {
        val named = annotation as Named
        return ConstraintProvider(NamedConstraint(named.name))
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
@UseFilterProviderFactory(NamedConstraintFactory::class)
annotation class Named(val name: String)
