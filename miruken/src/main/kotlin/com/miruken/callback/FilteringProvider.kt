package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import kotlin.reflect.KType

interface FilteringProvider {
    fun getFilters(
            binding:           MethodBinding,
            callbackType:      KType,
            logicalResultType: KType,
            composer:          Handling
    ) : List<Filtering<*,*>>
}