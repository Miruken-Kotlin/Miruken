package com.miruken.callback.policy

import com.miruken.runtime.isUnit

class ReturnUnit(rule: ReturnRule) : ReturnRuleDelegate(rule) {
    override fun matches(method: MethodDispatch): Boolean {
        val returnType = method.returnType
        return returnType.isUnit || rule.matches(method)
    }

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        if (bindingInfo.dispatch.callable.returnType.isUnit)
            rule.configure(bindingInfo)
    }
}

val ReturnRule.orUnit: ReturnRule get() = ReturnUnit(this)