package com.miruken.callback

fun Handling.getFilterOptions() : FilterOptions? {
    val options = FilterOptions()
    return handle(options, true) map { options }
}

fun Handling.withFilters(vararg filters: Filtering<*,*>): Handling {
    return FilterOptions(
            listOf(FilterInstanceProvider(filters.toList())))
            .decorate(this)
}

fun Handling.withFilterProviders(vararg providers: FilteringProvider): Handling {
    return FilterOptions(providers.toList())
            .decorate(this)
}