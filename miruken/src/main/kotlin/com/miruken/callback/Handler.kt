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
            composer: Handling
    ): HandleResult =
            dispatch(this, callback, greedy, composer)

    companion object {
        fun dispatch(
                handler:  Any,
                callback: Any,
                greedy:   Boolean,
                composer: Handling
        ): HandleResult {
            if (SkipTypes.contains(handler::class))
                return HandleResult(true)
            return if (callback is Dispatching)
                callback.dispatch(handler, greedy, composer)
            else HandlesPolicy.dispatch(handler, callback, greedy, composer)
        }

        fun toHandler(target: Any) : Handling =
                target as? Handling ?: HandlerAdapter(target)

        private val SkipTypes = setOf(Handler::class,
                CascadeHandler::class, CompositeHandler::class,
                CompositionScope::class)
    }
}