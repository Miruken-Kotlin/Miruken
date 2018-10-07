package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface FilteringProvider {
    val required: Boolean

    fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Filtering<*,*>>
}

interface Filtered {
    val filters: Collection<FilteringProvider>

    fun addFilters(vararg filters: Filtering<*,*>)
    fun addFilters(filters: Collection<Filtering<*,*>>)
    fun addFilterProviders(vararg providers: FilteringProvider)
    fun addFilterProviders(providers: Collection<FilteringProvider>)
}