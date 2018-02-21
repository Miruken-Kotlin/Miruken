package com.miruken.callback.policy

import com.miruken.runtime.isUnit
import kotlin.reflect.KParameter
import kotlin.reflect.KType

object ReturnsKey : ReturnRule {
    override fun matches(
            returnType: KType,
            parameters: List<KParameter>
    ): Boolean {
        return returnType.isUnit
    }
}