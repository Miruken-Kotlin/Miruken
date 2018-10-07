package com.miruken.callback.policy.bindings

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

class MetadataKeyConstraint(
        private val key:   Any,
        private val value: Any?
): BindingConstraint {

    override fun require(metadata: BindingMetadata) =
            metadata.set(key, value)

    override fun matches(metadata: BindingMetadata) =
            metadata.has(key) && metadata.get<Any>(key) == value
}