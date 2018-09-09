package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.callback.policy.MemberBinding
import kotlin.reflect.KClass
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
        filterType:   KType,
        binding:      MemberBinding,
        providers:    List<FilteringProvider>,
        useProviders: List<UseFilterProvider>,
        useFilters:   List<UseFilter>
): List<Filtering<*,*>>? {
    val options = getOptions(FilterOptions())
    val handler = when (options?.skipFilters) {
        true -> return when {
            binding.filters.any { it.required } ||
            providers.any       { it.required } ||
            useProviders.any    { it.required } ||
            useFilters.any      { it.required } -> null
            else -> emptyList()
        }
        null -> when (binding.skipFilters) {
            true -> return emptyList()
            else -> skipFilters()
        }
        else -> this
    }
    return handler.getOrderedFilters(filterType, binding,
            options, providers, useProviders, useFilters)
}

private fun Handling.getOrderedFilters(
        filterType:   KType,
        binding:      MemberBinding,
        options:      FilterOptions?,
        providers:    List<FilteringProvider>,
        useProviders: List<UseFilterProvider>,
        useFilters:   List<UseFilter>
): List<Filtering<*,*>> {
    val allProviders = (binding.filters + providers +
            useProviders.mapNotNull {
                getFilterProvider(it.filterProviderClass, this)
            }).toMutableList()

    options?.providers?.also { allProviders.addAll(it) }

    useFilters.takeIf { it.isNotEmpty() }?.also {
        allProviders.add(UseFiltersFilterProvider(it))
    }

    return allProviders.flatMap {
        it.getFilters(binding, filterType, this)
    }.sortedWith(OrderedComparator)
}

private fun getFilterProvider(
        providerClass: KClass<out FilteringProvider>,
        composer:      Handling
) = providerClass.objectInstance
        ?: composer.resolve(providerClass) as? FilteringProvider