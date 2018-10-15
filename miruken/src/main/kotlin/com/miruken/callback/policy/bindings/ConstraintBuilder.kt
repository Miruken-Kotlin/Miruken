package com.miruken.callback.policy.bindings

import com.miruken.callback.NamedConstraint

class ConstraintBuilder(metadata: BindingMetadata? = null) {
    private val _metadata = metadata ?: BindingMetadata()

    constructor(bindingScope: BindingScope)
        : this(bindingScope.metadata)

    infix fun named(name: String) = require(NamedConstraint(name))

    fun require(key: String, value: Any?): ConstraintBuilder
    {
        _metadata.set(key, value)
        return this
    }

    infix fun require(constraint: BindingConstraint): ConstraintBuilder {
        constraint.require(_metadata)
        return this
    }

    infix fun require(metadata: BindingMetadata): ConstraintBuilder {
        metadata.name?.also { _metadata.name = it }
        metadata.mergeInto(_metadata)
        return this
    }

    fun build() = _metadata
}