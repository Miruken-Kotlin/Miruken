package com.miruken.callback.policy

interface ArgumentRule {
    fun matches(argument: Argument) : Boolean

    fun configure(
            argument:    Argument,
            bindingInfo: PolicyMethodBindingInfo) {}

    fun resolve(callback: Any): Any?
}