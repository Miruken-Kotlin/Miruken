package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallableDispatch
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

interface InferringCallback {
    fun inferCallback(): Any
}

interface BatchingCallback {
    val canBatch: Boolean
}

interface FilteringCallback {
    val canFilter: Boolean
}

interface DispatchingCallbackGuard {
    fun canDispatch(
            target:     Any,
            dispatcher: CallableDispatch
    ): Boolean
}

interface DispatchingCallback {
    val policy: CallbackPolicy?

    fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult
}