package com.miruken.callback

import com.miruken.Flags

class CallbackSemantics(
        options: Flags<CallbackOptions>) : Composition(),
    ResolvingCallback, FilteringCallback, BatchingCallback {

    private var _specified = -CallbackOptions.NONE

    var options = options
        private set

    override val allowFiltering = false
    override val allowBatching  = false

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

    override fun getResolveCallback() = this

    fun mergeInto(semantics: CallbackSemantics) {
        mergeInto(semantics, CallbackOptions.DUCK)
        mergeInto(semantics, CallbackOptions.STRICT)
        mergeInto(semantics, CallbackOptions.BEST_EFFORT)
        mergeInto(semantics, CallbackOptions.BROADCAST)
    }

    fun mergeInto(semantics: CallbackSemantics,
                  option:    Flags<CallbackOptions>) {
        if (isSpecified(option) && !semantics.isSpecified(options))
            semantics.setOption(option, hasOption(options))
    }

    companion object {
        val NONE = CallbackSemantics(CallbackOptions.NONE)
    }
}