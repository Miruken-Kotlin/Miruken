package com.miruken.callback.policy

interface ReturnRule {
    fun matches(
            method:  CallableDispatch,
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

