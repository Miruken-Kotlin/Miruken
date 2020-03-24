package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class FilterInstanceProvider(
        override val required: Boolean,
        vararg       filters:  Filtering<*,*>
) : FilteringProvider {
    private val _filters = setOf(*filters)

    constructor(vararg filters: Filtering<*,*>)
        : this(false, *filters)

    override fun appliesTo(
            callback:     Any,
            callbackType: TypeReference?
    ): Boolean? = null

    override fun getFilters(
            binding:      MemberBinding,
            filterType:   KType,
            callback:     Any,
            callbackType: TypeReference?,
            composer:     Handling
    ) = _filters.filter {
        val filterConformance = it::class.getFilteringInterface()
        isCompatibleWith(filterConformance, filterType)
    }
}