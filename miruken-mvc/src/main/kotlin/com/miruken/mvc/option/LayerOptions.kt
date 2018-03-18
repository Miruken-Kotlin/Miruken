package com.miruken.mvc.option

import com.miruken.callback.Handling
import com.miruken.callback.Options
import com.miruken.mvc.view.ViewLayer
import java.lang.IndexOutOfBoundsException

class LayerOptions : Options<LayerOptions>() {
    var push:      Boolean? = null
    var overlay:   Boolean? = null
    var unload:    Boolean? = null
    var immediate: Boolean? = null
    var selector:  ((List<ViewLayer>) -> ViewLayer)? = null

    fun choose(viewLayers: List<ViewLayer>) =
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
    RegionOptions().apply {
        layer = LayerOptions().apply { push = true }
    }.decorate(this)

val Handling.overlay get() =
        RegionOptions().apply {
            layer = LayerOptions().apply { overlay = true }
        }.decorate(this)

val Handling.unloadRegion get() =
        RegionOptions().apply {
            layer = LayerOptions().apply { unload = true }
        }.decorate(this)

val Handling.displayImmediate get() =
        RegionOptions().apply {
            layer = LayerOptions().apply { immediate = true }
        }.decorate(this)

fun Handling.layer(viewLayer: ViewLayer) =
        RegionOptions().apply {
            layer = LayerOptions().apply {
                selector = { layers ->
                    if (!layers.contains(viewLayer)) {
                        throw IllegalArgumentException("Layer not found")
                    }
                    viewLayer
                }
            }
        }.decorate(this)

fun Handling.layer(offset: Int) =
        RegionOptions().apply {
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
        }.decorate(this)