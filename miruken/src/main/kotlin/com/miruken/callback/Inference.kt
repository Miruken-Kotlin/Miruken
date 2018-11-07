package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy

class Inference(
        callback:     Any,
        callbackType: TypeReference?
) : Trampoline(callback, callbackType),
        InferringCallback {

    private val _inferred by lazy(LazyThreadSafetyMode.NONE) {
        CallbackPolicy.getCallbackHandlers(
                callback, callbackType).map {
            Resolution(it, callback, callbackType)
        }
    }

    override val policy: CallbackPolicy? = null

    override fun inferCallback() = this

    override fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ) = _inferred.fold(
            super.dispatch(
                    handler, callbackType, greedy, composer)
    ) { result, infer ->
        if (result.stop || (result.handled && !greedy)) {
            return@fold result
        } else {
            result or infer.dispatch(
                    handler, callbackType, greedy, composer)
        }
    }
}

class InferringHandler(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val resolving     = getInferCallback(callback, callbackType)
        val resolvingType = if (resolving === callback) callbackType else null
        return handler.handle(resolving, resolvingType, greedy, composer)
    }

    private fun getInferCallback(
            callback:     Any,
            callbackType: TypeReference?
    ) = when (callback) {
        is InferringCallback -> callback.inferCallback()
        else -> Resolution.getResolving(callback, callbackType)
    }
}

