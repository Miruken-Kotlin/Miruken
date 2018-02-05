package com.miruken

open class Trampoline(val callback: Any) : IDispatchCallback {
    override val policy: CallbackPolicy?
        get() = (callback as? IDispatchCallback)?.policy

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: IHandler
    ) : HandleResult =
            Handler.dispatch(handler, callback, greedy, composer)
}