package com.miruken.callback

class FilterOptions : Options<FilterOptions>() {
    var skipFilters: Boolean? = null

    var providers: Collection<FilteringProvider>? = null

    override fun mergeInto(other: FilterOptions) {
        if (skipFilters != null && other.skipFilters == null)
            other.skipFilters = skipFilters

        if (providers != null) {
            other.providers = other.providers?.apply {
                this + providers!!
            } ?: providers
        }
    }
}