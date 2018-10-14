package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class FilterInstanceProvider(
        override val required: Boolean,
        vararg   val filters:  Filtering<*,*>
) : FilteringProvider {
    constructor(vararg filters: Filtering<*,*>)
        : this(false, *filters)

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ) = filters.filter {
        val filterConformance = it::class.getFilteringInterface()
        isCompatibleWith(filterConformance, filterType)
    }
}