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
            return when {
                ExcludeTypes.contains(handler::class) ->
                    HandleResult.NotHandled
                callback is Dispatching ->
                    callback.dispatch(handler, greedy, composer)
                else ->
                    HandlesPolicy.dispatch(handler, callback, greedy, composer)
            }
        }

        fun toHandler(target: Any) : Handling =
                target as? Handling ?: HandlerAdapter(target)

        private val ExcludeTypes = setOf(Handler::class,
                CascadeHandler::class, CompositeHandler::class,
                CompositionScope::class)
    }
}