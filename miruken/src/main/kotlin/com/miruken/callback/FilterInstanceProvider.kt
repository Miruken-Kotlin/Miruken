package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import kotlin.reflect.KType

class FilterInstanceProvider(
        private val filters: List<Filtering<*,*>>
) : FilteringProvider {

    override fun getFilters(
            binding:           MethodBinding,
            callbackType:      KType,
            logicalResultType: KType,
            composer:          Handling
    ): List<Filtering<*,*>> = filters
}