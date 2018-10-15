package com.miruken.callback

import com.miruken.Flags

class CallbackSemantics(
        options: Flags<CallbackOptions>) : Composition(),
    InferringCallback, FilteringCallback, BatchingCallback {

    private var _specified = options

    var options = options
        private set

    override val canFilter = false
    override val canBatch  = false

    constructor() : this(CallbackOptions.NONE)

    fun hasOption(options: Flags<CallbackOptions>) =
            this.options has options

    fun setOption(options: Flags<CallbackOptions>, enabled: Boolean = true)
    {
        this.options = options.set(options, enabled)
        _specified += options
    }

    fun isSpecified(options: Flags<CallbackOptions>) =
            _specified has options

    override fun inferCallback() = this

    fun mergeInto(semantics: CallbackSemantics) {
        mergeInto(semantics, CallbackOptions.DUCK)
        mergeInto(semantics, CallbackOptions.STRICT)
        mergeInto(semantics, CallbackOptions.BEST_EFFORT)
        mergeInto(semantics, CallbackOptions.BROADCAST)
    }

    private fun mergeInto(semantics: CallbackSemantics,
                          option:    Flags<CallbackOptions>) {
        if (isSpecified(option) && !semantics.isSpecified(options))
            semantics.setOption(option, hasOption(options))
    }

    companion object {
        val NONE = CallbackSemantics(CallbackOptions.NONE)
    }
}