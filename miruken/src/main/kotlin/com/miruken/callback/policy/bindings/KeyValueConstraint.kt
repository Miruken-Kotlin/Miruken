package com.miruken.callback.policy.bindings

class KeyValueConstraint(
        val key:   Any,
        val value: Any?
) : BindingConstraint {

    override fun require(metadata: BindingMetadata) =
            metadata.set(key, value)

    override fun matches(metadata: BindingMetadata) =
            metadata.get<Any>(key) == value
}