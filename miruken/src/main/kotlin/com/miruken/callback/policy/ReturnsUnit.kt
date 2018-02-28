package com.miruken.callback.policy

import com.miruken.runtime.isUnit

object ReturnsUnit : ReturnRule {
    override fun matches(method: CallableDispatch) =
            method.returnType.isUnit
}
