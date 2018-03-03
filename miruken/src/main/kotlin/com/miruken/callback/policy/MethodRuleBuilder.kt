package com.miruken.callback.policy

class MethodRuleBuilder(
        private vararg val argumentRules: ArgumentRule
) {
    private var returnRule: ReturnRule? = null

    infix fun returns(returnRule: ReturnRule) {
        this.returnRule = returnRule
    }

    fun build(): MethodRule = returnRule?.let {
        MethodRule(it, *argumentRules)
    } ?: MethodRule(*argumentRules)
}