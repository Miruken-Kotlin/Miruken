package com.miruken.callback

import com.miruken.callback.policy.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class InstanceFilterProvider(
        vararg val filters: Filtering<*,*>
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