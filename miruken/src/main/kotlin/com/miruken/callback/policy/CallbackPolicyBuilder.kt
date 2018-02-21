package com.miruken.callback.policy

import kotlin.reflect.KType

open class CallbackPolicyBuilder(
        val policy: CallbackPolicy, callbackType: KType) {

    val callback: CallbackArgument = CallbackArgument(callbackType)

    fun match(vararg arguments: ArgumentRule) {
        policy.addRule(MethodRule(*arguments))
    }

    fun match(returnRule: ReturnRule, vararg arguments: ArgumentRule) {
        policy.addRule(MethodRule(returnRule, *arguments))
    }

    fun matchCallback(vararg arguments: ArgumentRule) {
        if (!arguments.filterIsInstance<CallbackArgument>().any()) {
            match(*arguments, callback)
        }
        match(*arguments)
    }

    fun matchCallback(returnRule: ReturnRule, vararg arguments: ArgumentRule) {
        if (!arguments.filterIsInstance<CallbackArgument>().any()) {
            match(returnRule, *arguments, callback)
        }
        match(returnRule, *arguments)
    }
}