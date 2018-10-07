package com.miruken.callback.policy.bindings

interface BindingConstraint {
    fun require(metadata: BindingMetadata)
    fun matches(metadata: BindingMetadata): Boolean
}