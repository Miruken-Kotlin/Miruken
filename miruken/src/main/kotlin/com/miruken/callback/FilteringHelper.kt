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
        binding:      MethodBinding,
        filterType:   KType,
        useProviders: List<UseFilterProvider<*>>,
        useFilters:   List<UseFilter<*>>? = null
): Collection<Filtering<*,*>> {
    val providers = useProviders.mapNotNull {
        getFilterProvider(it.filterProviderClasses, this)
    }.toMutableList()

    getFilterOptions()?.also { providers.addAll(it.providers) }

    useFilters?.takeIf { it.isNotEmpty() }?.also {
        providers.add(UseFiltersFilterProvider(it))
    }

    return providers.flatMap {
        it.getFilters(binding, filterType, this)
    }.sortedWith(OrderedComparator)
}

private fun getFilterProvider(
        providerClass: KClass<out FilteringProvider>,
        composer:      Handling
) = providerClass.objectInstance
        ?: composer.resolve(providerClass) as? FilteringProvider