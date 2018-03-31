package com.miruken.validate

import com.miruken.callback.*

class ValidationOptions : Options<ValidationOptions>() {
    var stopOnFailure: Boolean? = null

    override fun mergeInto(other: ValidationOptions) {
        if (stopOnFailure != null && other.stopOnFailure == null)
            other.stopOnFailure = stopOnFailure
    }
}

fun Handling.stopOnFailure(stop: Boolean = true) =
        withOptions(ValidationOptions().apply { stopOnFailure = stop })