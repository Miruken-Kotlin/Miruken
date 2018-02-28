package com.miruken.callback.policy

class ReturnRuleDisjunction(
        private val leftSide:  ReturnRule,
        private val rightSide: ReturnRule
): ReturnRule {
    override fun matches(method: CallableDispatch): Boolean =
            leftSide.matches(method) || rightSide.matches(method)

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        if (leftSide.matches(bindingInfo.dispatcher))
            leftSide.configure(bindingInfo)
        else if (rightSide.matches(bindingInfo.dispatcher))
            rightSide.configure(bindingInfo)
    }
}