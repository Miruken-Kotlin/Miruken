package com.miruken.validate

import com.miruken.callback.policy.ContravariantPolicy
import com.miruken.callback.policy.PolicyMemberBinding
import com.miruken.callback.policy.UsePolicy
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@UsePolicy(ValidatesPolicy::class)
annotation class Validates(
        vararg val scopes: KClass<*> = [],
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
            binding:  PolicyMemberBinding
    ) = (callback as Validation).let {
        val validates = binding.annotation as Validates
       (it.outcome.isValid ||
         !(it.stopOnFailure || validates.skipIfInvalid)) &&
           it.satisfiesScopes(*validates.scopes)
    }
}
