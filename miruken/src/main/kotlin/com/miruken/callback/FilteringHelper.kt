package com.miruken.callback

import com.miruken.OrderedComparator
import com.miruken.callback.policy.MethodBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun Handling.getFilterOptions(): FilterOptions? {
    val options = FilterOptions()
    return handle(options, true) success { options }
}

fun Handling.withFilterProviders(vararg providers: FilteringProvider) =
        FilterOptions(*providers).decorate(this)

fun Handling.getOrderedFilters(
        filterType:   KType,
        binding:      MethodBinding,
        providers:    List<FilteringProvider>,
        useProviders: List<UseFilterProvider>,
        useFilters:   List<UseFilter>? = null
): List<Filtering<*,*>> {
    val allProviders = (providers + useProviders.mapNotNull {
        getFilterProvider(it.filterProviderClasses, this)
    }).toMutableList()

    getFilterOptions()?.also { allProviders.addAll(it.providers) }

    useFilters?.takeIf { it.isNotEmpty() }?.also {
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