package com.miruken.callback.policy

import com.miruken.callback.FilterInstanceProvider
import com.miruken.callback.Filtering
import com.miruken.callback.FilteringProvider
import kotlin.reflect.KType

open class CallbackPolicyBuilder(
        val policy: CallbackPolicy, callbackType: KType) {

    val callback: CallbackArgument = CallbackArgument(callbackType)

    fun filters(vararg filters: Filtering<*,*>) =
        policy.addFilters(FilterInstanceProvider(filters.toList()))

    fun pipeline(vararg providers: FilteringProvider) =
        policy.addFilters(*providers)

    fun matches(vararg arguments: ArgumentRule) : MethodRule {
        val rule = MethodRule(policy::bindMethod, *arguments)
        policy.addRule(rule)
        return rule
    }
}