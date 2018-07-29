package com.miruken.callback

import com.miruken.callback.policy.MemberBinding
import kotlin.reflect.KType

interface FilteringProvider {
    val required: Boolean

    fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>>
}