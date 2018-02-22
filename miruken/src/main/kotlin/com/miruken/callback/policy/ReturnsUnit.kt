package com.miruken.callback.policy

import com.miruken.runtime.isUnit

object ReturnsUnit : ReturnRule {
    override fun matches(method: MethodDispatch) =
            method.returnType.isUnit
}
