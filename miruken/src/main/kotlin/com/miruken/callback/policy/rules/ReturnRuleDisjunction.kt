package com.miruken.callback.policy.rules

import com.miruken.callback.policy.CallableDispatch
import com.miruken.callback.policy.bindings.PolicyMemberBindingInfo

class ReturnRuleDisjunction(
        private val leftSide: ReturnRule,
        private val rightSide: ReturnRule
): ReturnRule {
    override fun matches(method: CallableDispatch, context: RuleContext) =
            leftSide.matches(method, context) ||
            rightSide.matches(method, context)

    override fun configure(bindingInfo: PolicyMemberBindingInfo) {
        val context = RuleContext()
        if (leftSide.matches(bindingInfo.dispatcher, context))
            leftSide.configure(bindingInfo)
        else if (rightSide.matches(bindingInfo.dispatcher, context))
            rightSide.configure(bindingInfo)
    }
}