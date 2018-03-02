package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import kotlin.reflect.KType

class FilterInstanceProvider(
        vararg filters: Filtering<*,*>
) : FilteringProvider {
    private val _filters = filters.toList()

    override fun getFilters(
            binding:           MethodBinding,
            callbackType:      KType,
            logicalResultType: KType,
            composer:          Handling
    ): List<Filtering<*,*>> = _filters
}