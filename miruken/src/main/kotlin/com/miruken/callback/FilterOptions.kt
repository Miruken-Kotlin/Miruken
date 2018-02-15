package com.miruken.callback

class FilterOptions(
        var providers: List<FilteringProvider> = emptyList()
) : Options<FilterOptions>() {

    override fun mergeInto(other: FilterOptions) {
        other.providers += providers
    }
}