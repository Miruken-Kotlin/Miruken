package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import kotlin.reflect.KType

interface FilteringProvider {
    fun getFilters(
            binding:    MethodBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>>
}