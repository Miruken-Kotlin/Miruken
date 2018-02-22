package com.miruken.callback.policy

class ReturnRuleDisjunction(
        private val leftSide:  ReturnRule,
        private val rightSide: ReturnRule
): ReturnRule {
    override fun matches(method: MethodDispatch): Boolean =
            leftSide.matches(method) || rightSide.matches(method)

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        if (leftSide.matches(bindingInfo.dispatch))
            leftSide.configure(bindingInfo)
        else if (rightSide.matches(bindingInfo.dispatch))
            rightSide.configure(bindingInfo)
    }
}