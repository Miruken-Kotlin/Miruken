package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.callback.withOptions
import com.miruken.mvc.view.ViewingLayer
import java.lang.IndexOutOfBoundsException

class LayerOptions : Options<LayerOptions>() {
    var push:      Boolean? = null
    var overlay:   Boolean? = null
    var unload:    Boolean? = null
    var immediate: Boolean? = null
    var selector:  ((List<ViewingLayer>) -> ViewingLayer)? = null

    fun choose(viewLayers: List<ViewingLayer>) =
            selector?.invoke(viewLayers)

    override fun mergeInto(other: LayerOptions) {
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

val Handling.pushLayer get() =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply { push = true }
    })

val Handling.overlay get() =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply { overlay = true }
    })

val Handling.unloadRegion get() =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply { unload = true }
    })

val Handling.displayImmediate get() =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply { immediate = true }
    })

fun Handling.layer(viewLayer: ViewingLayer) =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply {
            selector = { layers ->
                if (!layers.contains(viewLayer)) {
                    throw IllegalArgumentException("Layer not found")
                }
                viewLayer
            }
        }
    })

fun Handling.layer(offset: Int) =
    withOptions(RegionOptions().apply {
        layer = LayerOptions().apply {
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