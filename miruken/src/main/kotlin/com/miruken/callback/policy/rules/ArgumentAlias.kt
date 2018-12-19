package com.miruken.callback.policy.rules

import com.miruken.callback.policy.Argument

class ArgumentAlias(
        rule: ArgumentRule,
        private val alias: String
) : ArgumentRuleDelegate(rule) {
    override fun matches(argument: Argument, context: RuleContext) =
            super.matches(argument, context) &&
            context.addAlias(alias, argument.parameterType)
}

operator fun ArgumentRule.invoke(alias: String) =
        ArgumentAlias(this, alias)