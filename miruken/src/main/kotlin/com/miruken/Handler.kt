package com.miruken

open class Handler : IHandler {
    override fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ) : HandleResult {

    }

    protected open fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: IHandler?
    ) : HandleResult {

    }

    companion object {
        fun dispatch(
                handler:  Any,
                callback: Any,
                greedy:   Boolean,
                composer: IHandler
        ) : HandleResult {

        }
    }
}