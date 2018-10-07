package com.miruken.callback.policy.rules

class MethodRuleBuilder(vararg val argumentRules: ArgumentRule) {
    private var returnRule: ReturnRule? = null

    val weight get() =
        argumentRules.size + (returnRule?.let { 1 } ?: 0)

    infix fun returns(returnRule: ReturnRule) {
        this.returnRule = returnRule
    }

    fun build(): MethodRule = returnRule?.let {
        MethodRule(it, *argumentRules)
    } ?: MethodRule(*argumentRules)
}