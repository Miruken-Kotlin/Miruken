package com.miruken.callback.policy

import com.miruken.TypeReference
import com.miruken.callback.FilteringProvider
import com.miruken.callback.policy.rules.*

abstract class CallbackPolicyBuilder(val callbackType: TypeReference) {
    private val _rules = mutableListOf<MethodRuleBuilder>()

    val any = ReturnRule.Anything

    val filters = mutableListOf<FilteringProvider>()

    val callback: CallbackArgument = CallbackArgument(callbackType)

    val rules: List<MethodRule> get() {
        _rules.sortByDescending { it.weight }
        return _rules.map { it.build() }
    }

    fun pipeline(vararg providers: FilteringProvider) =
            filters.addAll(providers)

    fun matches(vararg arguments: ArgumentRule): MethodRuleBuilder {
        val rule = MethodRuleBuilder(*arguments)
        _rules.add(rule)
        return rule
    }

    abstract fun build(): CallbackPolicy
}