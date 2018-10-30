package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.callback.withOptions

class RegionOptions : Options<RegionOptions>() {
    var tag:   Any?          = null
    var layer: LayerOptions? = null

    override fun mergeInto(other: RegionOptions) {
        layer?.run {
            val l = other.layer ?: LayerOptions()
            mergeInto(l)
            if (other.layer == null)
                other.layer = l
        }
    }
}

fun Handling.regionOptions(options: RegionOptions) = withOptions(options)