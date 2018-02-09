package com.miruken.callback

open class Handler : Handling {
    override fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult {
        val scope = composer ?:
            this as? CompositionScope ?: CompositionScope(this)
        return handleCallback(callback, greedy, scope)
    }

    protected open fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult =
            dispatch(this, callback, greedy, composer)

    companion object {
        fun dispatch(
                handler:  Any,
                callback: Any,
                greedy:   Boolean,
                composer: Handling?
        ): HandleResult {
            TODO("not implemented")
        }

        fun toHandler(target: Any) : Handling =
                target as? Handling ?: HandlerAdapter(target)
    }
}