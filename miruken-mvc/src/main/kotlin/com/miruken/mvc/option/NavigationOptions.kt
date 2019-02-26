package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.callback.withOptions

class NavigationOptions : Options<NavigationOptions>() {
    var region: RegionOptions? = null
    var noBack: Boolean?       = null
    var goBack: Boolean?       = null

    override fun mergeInto(other: NavigationOptions) {
        if (noBack != null && other.noBack == null)
            other.noBack = noBack
        if (goBack != null && other.goBack == null)
            other.goBack = goBack
        region?.run {
            val l = other.region ?: RegionOptions()
            mergeInto(l)
            if (other.region == null)
                other.region = l
        }
    }
}

val Handling.noBack get() =
    withOptions(NavigationOptions().apply { noBack = true })

val Handling.goBack get() =
    withOptions(NavigationOptions().apply { goBack = true })

fun Handling.navigationOptions(options: NavigationOptions) =
        withOptions(options)