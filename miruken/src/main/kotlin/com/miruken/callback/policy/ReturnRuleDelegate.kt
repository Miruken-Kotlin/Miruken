package com.miruken.callback.policy

open class ReturnRuleDelegate(val rule: ReturnRule) : ReturnRule by rule {
    override fun <R : ReturnRule> getInnerRule(): R? =
            super.getInnerRule() ?: rule.getInnerRule()

}