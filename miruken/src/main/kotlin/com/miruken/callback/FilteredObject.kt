package com.miruken.callback

import kotlin.reflect.KAnnotatedElement

abstract class FilteredObject() : Filtered {
    private val _filters = lazy {
        mutableSetOf<FilteringProvider>()
    }

    constructor(annotatedElement: KAnnotatedElement) : this() {
        addFilters(annotatedElement.getFilterProviders())
    }

    constructor(filters: Collection<FilteringProvider>) : this() {
        addFilters(filters)
    }

    final override val filters: Collection<FilteringProvider>
        get() = if (_filters.isInitialized()) _filters.value else emptyList()

    final override fun addFilters(vararg providers: FilteringProvider) {
        _filters.value.addAll(providers)
    }

    final override fun addFilters(providers: Collection<FilteringProvider>) {
        _filters.value.addAll(providers)
    }

    final override fun removeFilters(vararg providers: FilteringProvider) {
        if (_filters.isInitialized()) {
            _filters.value.removeAll(providers)
        }
    }

    final override fun removeFilters(providers: Collection<FilteringProvider>) {
        if (_filters.isInitialized()) {
            _filters.value.removeAll(providers)
        }
    }

    override fun removeAllFilters() {
        if (_filters.isInitialized()) {
            _filters.value.clear()
        }
    }
}