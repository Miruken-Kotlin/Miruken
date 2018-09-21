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

interface FilterValidating {
    fun validate(binding: MemberBinding)
}

interface Filtered {
    val filters: Collection<FilteringProvider>

    fun addFilters(vararg filters: Filtering<*,*>)
    fun addFilters(filters: Collection<Filtering<*,*>>)
    fun addFilterProviders(vararg providers: FilteringProvider)
    fun addFilterProviders(providers: Collection<FilteringProvider>)
}