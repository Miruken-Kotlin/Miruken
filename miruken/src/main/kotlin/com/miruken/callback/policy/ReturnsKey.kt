package com.miruken.callback.policy

object ReturnsKey : ReturnRule {
    override fun matches(method: MethodDispatch) =
            method.returnsSomething

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {

    }
}