package com.miruken.callback.policy.rules

import com.miruken.callback.policy.Argument
import kotlin.reflect.KClass

class ClassArgument(
        rule: ArgumentRule,
        private val kclass:  KClass<*>,
        private vararg val aliases: String?
): ArgumentRuleDelegate(rule) {
    override fun matches(
            argument: Argument,
            context: RuleContext
    ):Boolean {
        val parameterType = argument.parameterType
        if (parameterType.classifier != kclass) {
            return false
        }
        if (aliases.isEmpty()) {
            return super.matches(argument, context)
        }
        if (aliases.size > kclass.typeParameters.size) {
            context.addError("$kclass has ${kclass.typeParameters.size} args, but ${aliases.size} provided")
        }
        return super.matches(argument, context) &&
                aliases.zip(parameterType.arguments) { alias, arg ->
                    alias.isNullOrEmpty() || (arg.type != null &&
                            context.addAlias(alias, arg.type!!))
                }.all { it }
    }
}

fun ArgumentRule.of(
        kclass: KClass<*>,
        vararg aliases: String?
) = ClassArgument(this, kclass, *aliases)