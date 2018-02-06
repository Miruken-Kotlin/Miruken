package com.miruken.callback

open class Handler : IHandler {
    override fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ): HandleResult {
        val scope = composer ?:
            this as? CompositionScope ?: CompositionScope(this)
        return handleCallback(callback, greedy, scope)
    }

    protected open fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ): HandleResult =
            dispatch(this, callback, greedy, composer)

    companion object {
        fun dispatch(
                handler:  Any,
                callback: Any,
                greedy:   Boolean,
                composer: IHandler?
        ): HandleResult {
            TODO("not implemented")
        }

        fun toHandler(target: Any) : IHandler =
                target as? IHandler ?: HandlerAdapter(target)
    }
}