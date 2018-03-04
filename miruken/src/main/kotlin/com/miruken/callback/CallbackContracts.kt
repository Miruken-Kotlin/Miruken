package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import kotlin.reflect.KType

interface Callback {
    val resultType: KType?
    var result:     Any?
    fun getCallbackKey(): Any? = null
}

interface AsyncCallback {
    val isAsync:    Boolean
    val wantsAsync: Boolean
}

interface BoundingCallback {
    val bounds: Any?
}

interface ResolvingCallback {
    fun getResolveCallback() : Any
}

interface BatchingCallback {
    val allowBatching: Boolean
}

interface FilteringCallback {
    val allowFiltering: Boolean
}

interface DispatchingCallback {
    val policy: CallbackPolicy?

    fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ) : HandleResult
}