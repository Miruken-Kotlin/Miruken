package com.miruken.callback

abstract class FilteredObject : Filtered {
    private val _filters by lazy {
        mutableSetOf<FilteringProvider>()
    }

    override val filters: Collection<FilteringProvider>
        get() = _filters

    final override fun addFilters(
            vararg providers: FilteringProvider
    ) {
        _filters.addAll(providers)
    }

    final override fun addFilters(
            providers: Collection<FilteringProvider>
    ) {
        _filters.addAll(providers)
    }
}