package com.miruken.callback

import com.miruken.callback.policy.bindings.BindingConstraint
import com.miruken.callback.policy.bindings.BindingMetadata

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR)
annotation class Named(val name: String)

class NamedConstraint(val name: String) : BindingConstraint {

    override fun require(metadata: BindingMetadata) {
        metadata.name = name
    }

    override fun matches(metadata: BindingMetadata) =
            metadata.name == null || metadata.name == name
}
