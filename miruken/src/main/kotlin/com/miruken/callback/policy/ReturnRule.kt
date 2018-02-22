package com.miruken.callback.policy

interface ReturnRule {
    fun matches(method: MethodDispatch) : Boolean

    fun configure(bindingInfo: PolicyMethodBindingInfo) {}

    @Suppress("UNCHECKED_CAST")
    fun <R: ReturnRule> getInnerRule() = this as? R
}