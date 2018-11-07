package com.miruken.callback.policy.rules

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch
import com.miruken.typeOf
import kotlin.reflect.full.isSupertypeOf

class ReturnsType(
        rule: ReturnRule,
        private val type: TypeReference
): ReturnRuleDelegate(rule) {
    override fun matches(
            method: CallableDispatch,
            context: RuleContext
    ) = type.kotlinType.isSupertypeOf(method.returnType) &&
            super.matches(method, context)
}

inline fun <reified T> ReturnRule.of() =
        ReturnsType(this, typeOf<T>())

fun ReturnRule.of(type: TypeReference) = ReturnsType(this, type)