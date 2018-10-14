package com.miruken.callback

abstract class FilteredObject : Filtered {
    private var _filters = emptyList<FilteringProvider>()

    override val filters: Collection<FilteringProvider>
        get() = _filters

    final override fun addFilters(
            vararg providers: FilteringProvider
    ) {
        _filters += providers
    }

    final override fun addFilters(
            providers: Collection<FilteringProvider>
    ) {
        _filters += providers
    }
}