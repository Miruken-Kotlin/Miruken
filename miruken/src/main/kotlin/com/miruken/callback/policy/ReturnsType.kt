package com.miruken.callback.policy

import com.miruken.typeOf
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf

class ReturnsType(
        rule: ReturnRule,
        private val type: KType
): ReturnRuleDelegate(rule) {
    override fun matches(
            method:  CallableDispatch,
            context: RuleContext
    ) = type.isSupertypeOf(method.returnType) &&
            super.matches(method, context)
}

inline fun <reified T> ReturnRule.of() =
        ReturnsType(this, typeOf<T>())

fun ReturnRule.of(type: KType) = ReturnsType(this, type)