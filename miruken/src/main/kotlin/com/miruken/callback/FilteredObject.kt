package com.miruken.callback

abstract class FilteredObject : Filtered {
    private var _filters = emptyList<FilteringProvider>()

    override val filters: Collection<FilteringProvider>
        get() = _filters

    final override fun addFilters(
            vararg filters: FilterSpec
    ) {
        if (filters.isNotEmpty()) {
            _filters += FilterSpecProvider(filters.toList())
        }
    }

    final override fun addFilters(
            filters: Collection<FilterSpec>
    ) {
        if (filters.isNotEmpty()) {
            _filters += FilterSpecProvider(filters)
        }
    }

    final override fun addFilterProviders(
            vararg providers: FilteringProvider
    ) {
        _filters += providers
    }

    final override fun addFilterProviders(
            providers: Collection<FilteringProvider>
    ) {
        _filters += providers
    }
}