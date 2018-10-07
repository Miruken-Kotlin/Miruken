package com.miruken.callback.policy.rules

import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

interface ReturnRule {
    fun matches(
            method: CallableDispatch,
            context: RuleContext
    ): Boolean

    fun configure(bindingInfo: PolicyMemberBindingInfo) {
    }

    infix fun and(otherRule: ReturnRule) =
            ReturnRuleConjunction(this, otherRule)

    infix fun or(otherRule: ReturnRule) =
            ReturnRuleDisjunction(this, otherRule)

    object Anything : ReturnRule {
        override fun matches(
                method: CallableDispatch,
                context: RuleContext
        ) = true
    }
}

