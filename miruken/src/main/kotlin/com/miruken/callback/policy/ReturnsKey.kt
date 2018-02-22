package com.miruken.callback.policy

import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit

object ReturnsKey : ReturnRule {
    override fun matches(method: MethodDispatch): Boolean {
        val returnType = method.returnType
        return !returnType.isUnit && !returnType.isNothing
    }

    override fun configure(bindingInfo: PolicyMethodBindingInfo) {

    }
}