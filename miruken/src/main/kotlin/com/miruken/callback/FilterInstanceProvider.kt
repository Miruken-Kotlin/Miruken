package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class FilterInstanceProvider(
        vararg   val filters:  Filtering<*,*>,
        override val required: Boolean = false
) : FilteringProvider {
    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ) = filters.filter {
        val filterConformance = it::class.getFilteringInterface()
        isCompatibleWith(filterConformance, filterType)
    }
}