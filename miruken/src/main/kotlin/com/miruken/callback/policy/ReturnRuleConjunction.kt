package com.miruken.callback.policy

class ReturnRuleConjunction(
        private val leftSide:  ReturnRule,
        private val rightSide: ReturnRule
): ReturnRule {
    override fun matches(method: MethodDispatch): Boolean =
        leftSide.matches(method) && rightSide.matches(method)

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        leftSide.configure(bindingInfo)
        rightSide.configure(bindingInfo)
    }
}