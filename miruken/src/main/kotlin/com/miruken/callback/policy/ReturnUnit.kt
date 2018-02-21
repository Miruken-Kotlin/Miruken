package com.miruken.callback.policy

import com.miruken.runtime.isUnit
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class ReturnUnit(rule: ReturnRule) : ReturnRuleDelegate(rule) {
    override fun matches(
            returnType: KType,
            parameters: List<KParameter>
    ): Boolean {
        return returnType.isUnit ||
                super.matches(returnType, parameters)
    }

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {
        if (bindingInfo.dispatch.callable.returnType.isUnit)
            rule.configure(bindingInfo)
    }
}

val ReturnRule.orUnit: ReturnRule get() = ReturnUnit(this)