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

    fun match(vararg arguments: ArgumentRule) =
        policy.addRule(MethodRule(policy::bindMethod, *arguments))

    fun match(returnRule: ReturnRule, vararg arguments: ArgumentRule) =
        policy.addRule(MethodRule(policy::bindMethod, returnRule, *arguments))

    fun matchWithCallback(vararg arguments: ArgumentRule) {
        if (!arguments.filterIsInstance<CallbackArgument>().any()) {
            match(*arguments, callback)
        }
        match(*arguments)
    }

    fun matchWithCallback(returnRule: ReturnRule, vararg arguments: ArgumentRule) {
        if (!arguments.filterIsInstance<CallbackArgument>().any()) {
            match(returnRule, *arguments, callback)
        }
        match(returnRule, *arguments)
    }
}