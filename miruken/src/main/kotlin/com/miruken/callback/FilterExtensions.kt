package com.miruken.callback

fun Handling.getFilterOptions() : FilterOptions? {
    val options = FilterOptions()
    return handle(options, true) success { options }
}

fun Handling.withFilters(vararg filters: Filtering<*,*>) =
        FilterOptions(FilterInstanceProvider(*filters))
            .decorate(this)

fun Handling.withFilterProviders(vararg providers: FilteringProvider) =
        FilterOptions(*providers).decorate(this)
