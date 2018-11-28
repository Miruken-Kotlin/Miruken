package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.callback.withOptions

class NavigationOptions : Options<NavigationOptions>() {
    var region: RegionOptions? = null
    var noBack: Boolean?       = null

    override fun mergeInto(other: NavigationOptions) {
        if (noBack != null && other.noBack == null)
            other.noBack = noBack
        region?.run {
            val l = other.region ?: RegionOptions()
            mergeInto(l)
            if (other.region == null) {
                other.region = l
            }
        }
    }
}

val Handling.noBack get() =
    withOptions(NavigationOptions().apply { noBack = true })

fun Handling.regionOptions(options: NavigationOptions) =
        withOptions(options)