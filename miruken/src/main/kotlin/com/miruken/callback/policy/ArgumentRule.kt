package com.miruken.callback.policy

interface ArgumentRule {
    fun matches(
            argument: Argument,
            context:  RuleContext
    ): Boolean

    fun configure(
            argument:    Argument,
            bindingInfo: PolicyMemberBindingInfo) {
    }

    fun resolve(callback: Any): Any?
}