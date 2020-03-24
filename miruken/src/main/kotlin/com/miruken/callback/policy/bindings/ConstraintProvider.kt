package com.miruken.callback.policy.bindings

import com.miruken.TypeReference
import com.miruken.callback.FilteringProvider
import com.miruken.callback.Handling
import kotlin.reflect.KType

open class ConstraintProvider(
        val constraint: BindingConstraint
): FilteringProvider {
    override val required = true

    override fun appliesTo(
            callback:     Any,
            callbackType: TypeReference?
    ) = callback is BindingScope

    override fun getFilters(
            binding:      MemberBinding,
            filterType:   KType,
            callback:     Any,
            callbackType: TypeReference?,
            composer:     Handling
    ) = filters

    companion object {
        private val filters = listOf(ConstraintFilter<Any>())
    }
}
