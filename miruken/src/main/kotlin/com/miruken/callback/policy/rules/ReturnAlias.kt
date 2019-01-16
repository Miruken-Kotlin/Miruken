package com.miruken.callback.policy.rules

import com.miruken.callback.policy.CallableDispatch

class ReturnAlias(rule: ReturnRule, private val alias: String) : ReturnRuleDelegate(rule) {
    override fun matches(method: CallableDispatch, context: RuleContext) =
            super.matches(method, context) &&
            context.addAlias(alias, method.returnType)
}

operator fun ReturnRule.invoke(alias: String) =
        ReturnAlias(this, alias)