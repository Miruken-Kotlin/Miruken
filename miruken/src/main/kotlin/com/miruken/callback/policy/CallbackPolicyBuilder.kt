package com.miruken.callback.policy

import com.miruken.callback.FilterInstanceProvider
import com.miruken.callback.Filtering
import com.miruken.callback.FilteringProvider
import java.lang.invoke.MethodHandle
import kotlin.reflect.KType

abstract class CallbackPolicyBuilder(val callbackType: KType) {
    private val _rules = mutableListOf<MethodRuleBuilder>()

    val filters = mutableListOf<FilteringProvider>()

    val callback: CallbackArgument = CallbackArgument(callbackType)

    val rules: List<MethodRule>
        get() {
        _rules.sortByDescending { it.weight }
        return _rules.map { it.build() }
    }

    fun filters(vararg filter: Filtering<*,*>) =
            filters.add(FilterInstanceProvider(*filter))

    fun pipeline(vararg providers: FilteringProvider) =
            filters.addAll(providers)

    fun matches(vararg arguments: ArgumentRule): MethodRuleBuilder {
        val rule = MethodRuleBuilder(*arguments)
        _rules.add(rule)
        return rule
    }

    abstract fun build(): CallbackPolicy
}