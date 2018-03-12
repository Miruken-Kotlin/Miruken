package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class InstanceFilterProvider(
        vararg val filters: Filtering<*,*>
) : FilteringProvider {
    override fun getFilters(
            binding:    MethodBinding,
            filterType: KType,
            composer:   Handling
    ) = filters.filter {
        val filterConformance = it::class.getFilteringInterface()
        isCompatibleWith(filterConformance, filterType)
    }
}