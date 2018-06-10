package com.miruken.callback.policy

import kotlin.reflect.KClass

class ReturnsClass(
        rule: ReturnRule,
        private val kclass: KClass<*>,
        private vararg val aliases: String?
): ReturnRuleDelegate(rule) {
    override fun matches(
            method:  CallableDispatch,
            context: RuleContext
    ): Boolean {
        val returnType = method.returnType
        if (returnType.classifier != kclass) {
            return false
        }
        if (aliases.isEmpty()) {
            return super.matches(method, context)
        }
        if (aliases.size > kclass.typeParameters.size) {
            context.addError("$kclass has ${kclass.typeParameters.size} args, but ${aliases.size} provided")
        }
        return super.matches(method, context) &&
                aliases.zip(returnType.arguments) { alias, arg ->
                    alias.isNullOrEmpty() || (arg.type != null &&
                            context.addAlias(alias!!, arg.type!!))
                }.all { it }
    }
}

fun ReturnRule.of(kclass: KClass<*>, vararg aliases: String?) =
        ReturnsClass(this, kclass, *aliases)