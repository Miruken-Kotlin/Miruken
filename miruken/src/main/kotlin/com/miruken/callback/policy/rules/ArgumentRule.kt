package com.miruken.callback.policy.rules

import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

interface ArgumentRule {
    fun matches(
            argument: Argument,
            context:  RuleContext
    ): Boolean

    fun configure(
            argument:    Argument,
            bindingInfo: PolicyMemberBindingInfo) {
    }

    fun resolve(callback: Any): Any?
}