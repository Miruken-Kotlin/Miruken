package com.miruken.validate

import com.miruken.callback.policy.ContravariantPolicy
import com.miruken.callback.policy.PolicyMethodBinding
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy(ValidatesPolicy::class)
annotation class Validates(
        val scopes:        Array<String> = [],
        val skipIfInvalid: Boolean = false
)

object ValidatesPolicy : ContravariantPolicy({
    target { cb: Validation -> cb.target } rules {
        matches (target, callback, extract { outcome })
        matches (target, extract { outcome })
        matches (target, callback)
        matches (callback)
    }
}) {
    override fun approve(
            callback: Any,
            binding:  PolicyMethodBinding
    ) = (callback as? Validation)?.let {
        val validates = binding.annotation as Validates
        val scope = validates.scopes.takeIf { it.isNotEmpty() }
        return (it.outcome.isValid ||
                !(it.stopOnFailure || validates.skipIfInvalid))
                && it.scopeMatcher.matches(scope)
    } ?: false
}
