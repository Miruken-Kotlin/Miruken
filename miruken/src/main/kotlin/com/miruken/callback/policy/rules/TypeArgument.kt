package com.miruken.callback.policy.rules

import com.miruken.TypeReference
import com.miruken.callback.policy.Argument
import com.miruken.typeOf
import kotlin.reflect.full.isSupertypeOf

class TypeArgument(
        rule: ArgumentRule,
        private val type: TypeReference
): ArgumentRuleDelegate(rule) {
    override fun matches(
            argument: Argument,
            context: RuleContext
    ) = type.kotlinType.isSupertypeOf(argument.parameterType) &&
            super.matches(argument, context)
}

inline fun <reified T> ArgumentRule.of() = TypeArgument(this, typeOf<T>())