package com.miruken.callback

class FilterOptions(
        vararg providers: FilteringProvider
) : Options<FilterOptions>() {
    var providers = providers.toList()

    override fun mergeInto(other: FilterOptions) {
        other.providers += providers
    }
}