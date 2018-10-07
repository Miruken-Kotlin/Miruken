package com.miruken.callback.policy.rules

import com.miruken.callback.policy.Argument
import com.miruken.typeOf
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf

class TypeArgument(
        rule: ArgumentRule,
        private val type: KType
): ArgumentRuleDelegate(rule) {
    override fun matches(
            argument: Argument,
            context: RuleContext
    ) = type.isSupertypeOf(argument.parameterType) &&
            super.matches(argument, context)
}

inline fun <reified T> ArgumentRule.of() =
        TypeArgument(this, typeOf<T>())