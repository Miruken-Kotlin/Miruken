package com.miruken.callback

import com.miruken.Flags

object CallbackOptions : Flags<CallbackOptions>() {
    val NONE        = Flags<CallbackOptions>(0)
    val DUCK        = Flags<CallbackOptions>(1 shl 0)
    val STRICT      = Flags<CallbackOptions>(1 shl 1)
    val BROADCAST   = Flags<CallbackOptions>(1 shl 2)
    val BEST_EFFORT = Flags<CallbackOptions>(1 shl 3)
    val NOTIFY      = BROADCAST + BEST_EFFORT
}

class CallbackSemantics(
        options: Flags<CallbackOptions>) : Composition(),
    ResolvingCallback, FilteringCallback, BatchingCallback {

    private var _specified = CallbackOptions.NONE

    var options = options
        private set

    override val allowFiltering = false
    override val allowBatching  = false

    constructor() : this(CallbackOptions.NONE)

    fun hasOptions(options: CallbackOptions) =
            this.options hasFlag options

    fun setOptions(options: CallbackOptions, enabled: Boolean = true)
    {
        this.options = options.setFlag(options, enabled)
        _specified += options
    }

    override fun getResolveCallback() = this
}