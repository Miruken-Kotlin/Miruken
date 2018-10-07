package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.callback.policy.bindings.MemberBinding
import kotlin.reflect.KType

fun Handling.skipFilters(skip: Boolean = true) =
        withOptions(FilterOptions().apply {
            skipFilters = skip
        })

fun Handling.enableFilters(enable: Boolean = true) =
        withOptions(FilterOptions().apply {
            skipFilters = !enable
        })

fun Handling.withFilters(vararg filters: Filtering<*,*>) =
        withOptions(FilterOptions().apply {
            providers = listOf(InstanceFilterProvider(*filters))
        })

fun Handling.withFilterProviders(vararg providers: FilteringProvider) =
        withOptions(FilterOptions().apply {
            this.providers = providers.toList()
        })

fun Handling.getOrderedFilters(
        filterType:      KType,
        binding:         MemberBinding,
        filterProviders: List<FilteringProvider>
): List<Filtering<*,*>>? {
    val options   = getOptions(FilterOptions())
    val providers = filterProviders +
            (options?.providers ?: emptyList())

    val handler = when (options?.skipFilters) {
        true -> return when {
            binding.filters.any { it.required } ||
            providers.any       { it.required } -> null
            else -> emptyList()
        }
        null -> when (binding.skipFilters) {
            true -> return emptyList()
            else -> skipFilters()
        }
        else -> this
    }
    return providers.flatMap {
        it.getFilters(binding, filterType, handler)
    }.sortedWith(OrderedComparator)
}

