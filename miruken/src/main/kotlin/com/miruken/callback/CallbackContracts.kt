package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import kotlin.reflect.KClass

interface Callback {
    val resultType: KClass<*>?
    var result:     Any?
}

interface AsyncCallback {
    val isAsync:    Boolean
    val wantsAsync: Boolean
}

interface Bounding {
    val bounds: Any
}

interface Resolving {
    fun getResolveCallback() : Any
}

interface Batching {
    val allowBatching: Boolean
}

interface Filtering {
    val allowFiltering: Boolean
}

interface Dispatching {
    val policy: CallbackPolicy?

    fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ) : HandleResult
}