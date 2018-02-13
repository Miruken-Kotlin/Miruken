package com.miruken.callback.policy

import com.miruken.callback.HandleResult
import com.miruken.callback.Handling

abstract class CallbackPolicy : Comparator<Any> {
    abstract fun getKey(callback: Any) : Any?

    abstract fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any>

    fun dispatch(
            handler:  Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  ((Any, Boolean) -> Boolean)? = null
    ): HandleResult {
        TODO("not implemented")
    }
}