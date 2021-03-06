package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.bindings.MemberBinding
import kotlin.reflect.KType

interface FilteringProvider {
    val required: Boolean

    fun appliesTo(
            callback:     Any,
            callbackType: TypeReference?
    ) : Boolean?

    fun getFilters(
            binding:      MemberBinding,
            filterType:   KType,
            callback:     Any,
            callbackType: TypeReference?,
            composer:     Handling
    ): List<Filtering<*,*>>?

    fun configure(owner: Any) {}
}

interface Filtered {
    val filters: Collection<FilteringProvider>

    fun addFilters(vararg providers: FilteringProvider)
    fun addFilters(providers: Collection<FilteringProvider>)
    fun removeFilters(vararg providers: FilteringProvider)
    fun removeFilters(providers: Collection<FilteringProvider>)
    fun removeAllFilters()
}