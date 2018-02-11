package com.miruken.callback

open class Resolution(key: Any, val callback: Any)
    : Inquiry(key, true), Resolving {

    private var _handled = false

    override fun getResolveCallback(): Any = this

    override fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        if (_handled && !greedy) return true
        _handled = Handler.dispatch(
                resolution, callback, greedy, composer
        ).handled || _handled
        return _handled
    }

    companion object {
        fun getDefaultResolvingCallback(callback: Any) : Any {
            TODO("not implemented")
        }
    }
}