package com.miruken.callback.policy

import com.miruken.callback.FilterInstanceProvider
import com.miruken.callback.Filtering
import com.miruken.callback.FilteringProvider
import kotlin.reflect.KType

abstract class CallbackPolicyBuilder(val callbackType: KType) {
    val rules   = mutableListOf<MethodRule>()
    val filters = mutableListOf<FilteringProvider>()

    val callback: CallbackArgument = CallbackArgument(callbackType)

    fun filters(vararg filter: Filtering<*,*>) =
            filters.add(FilterInstanceProvider(*filter))

    fun pipeline(vararg providers: FilteringProvider) =
            filters.addAll(providers)

    fun matches(vararg arguments: ArgumentRule): MethodRule {
        val rule = MethodRule(*arguments)
        rules.add(rule)
        return rule
    }

    abstract fun build(): CallbackPolicy
}