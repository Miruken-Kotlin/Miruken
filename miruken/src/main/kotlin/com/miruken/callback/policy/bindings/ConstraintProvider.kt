package com.miruken.callback.policy.bindings

import com.miruken.callback.FilteringProvider
import com.miruken.callback.Handling
import kotlin.reflect.KType

class ConstraintProvider(
        val constraint: BindingConstraint
): FilteringProvider {
    private val _constraint = listOf(ConstraintFilter<Any>())

    override val required = true

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ) = _constraint
}
