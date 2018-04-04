package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.callback.policy.MemberBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun Handling.withFilters(vararg filters: Filtering<*,*>) =
        withOptions(FilterOptions(InstanceFilterProvider(*filters)))

fun Handling.withFilterProviders(vararg providers: FilteringProvider) =
        withOptions(FilterOptions(*providers))

fun Handling.getOrderedFilters(
        filterType:   KType,
        binding:      MemberBinding,
        providers:    List<FilteringProvider>,
        useProviders: List<UseFilterProvider>,
        useFilters:   List<UseFilter>
): List<Filtering<*,*>> {
    val allProviders = (providers + useProviders.mapNotNull {
        getFilterProvider(it.filterProviderClass, this)
    }).toMutableList()

    getOptions(FilterOptions())
            ?.also { allProviders.addAll(it.providers) }

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