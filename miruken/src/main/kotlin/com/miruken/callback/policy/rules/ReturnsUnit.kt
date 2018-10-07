package com.miruken.callback.policy.rules

import com.miruken.callback.policy.CallableDispatch
import com.miruken.runtime.isUnit

object ReturnsUnit : ReturnRule {
    override fun matches(
            method: CallableDispatch,
            context: RuleContext
    ) = method.returnType.isUnit
}
