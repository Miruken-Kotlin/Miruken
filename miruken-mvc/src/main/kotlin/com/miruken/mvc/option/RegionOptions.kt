package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.callback.withOptions
import com.miruken.mvc.view.ViewingLayer

class RegionOptions : Options<RegionOptions>() {
    var tag:       Any?     = null
    var push:      Boolean? = null
    var overlay:   Boolean? = null
    var unload:    Boolean? = null
    var immediate: Boolean? = null
    var selector:  ((List<ViewingLayer>) -> ViewingLayer)? = null

    fun choose(viewLayers: List<ViewingLayer>) =
            selector?.invoke(viewLayers)

    override fun mergeInto(other: RegionOptions) {
        if (tag != null && other.tag == null)
            other.tag = tag
        if (push != null && other.push == null)
            other.push = push
        if (overlay != null && other.overlay == null)
            other.overlay = overlay
        if (unload != null && other.unload == null)
            other.unload = unload
        if (immediate != null && other.immediate == null)
            other.immediate = immediate
        if (selector != null && other.selector == null)
            other.selector = selector
    }
}

fun Handling.region(tag: String) =
        withOptions(NavigationOptions().apply {
            region = RegionOptions().apply { this.tag = tag }
        })

val Handling.pushLayer get() =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply { push = true }
    })

val Handling.overlay get() =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply { overlay = true }
    })

val Handling.unloadRegion get() =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply { unload = true }
    })

val Handling.displayImmediate get() =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply { immediate = true }
    })

fun Handling.layer(viewLayer: ViewingLayer) =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply {
            selector = { layers ->
                if (!layers.contains(viewLayer)) {
                    throw IllegalArgumentException("Layer not found")
                }
                viewLayer
            }
        }
    })

fun Handling.layer(offset: Int) =
    withOptions(NavigationOptions().apply {
        region = RegionOptions().apply {
            selector = { layers ->
                val index = when {
                    offset < 0 -> layers.size + offset - 1
                    else -> offset
                }
                if (index < 0 || index >= layers.size) {
                    throw IndexOutOfBoundsException()
                }
                layers[index]
            }
        }
    })