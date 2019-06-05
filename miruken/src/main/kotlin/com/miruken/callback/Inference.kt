package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.callback.policy.HandlerDescriptorFactory

class Inference(
        callback:     Any,
        callbackType: TypeReference?
) : Trampoline(callback, callbackType), InferringCallback {

    private val _inferred by lazy(LazyThreadSafetyMode.NONE) {
        HandlerDescriptorFactory.current.getCallbackHandlers(
                CallbackPolicy.getCallbackPolicy(callback),
                callback, callbackType).map {
            Resolution(it.handlerClass, callback, callbackType)
        }
    }

    override fun inferCallback() = this

    override fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val direct = super.dispatch(
                handler, callbackType, greedy, composer)
        if (direct.handled) return direct
        return _inferred.fold(direct) { result, infer ->
            if (result.stop || (result.handled && !greedy)) {
                return@fold result
            } else {
                result or infer.dispatch(
                        handler, callbackType, greedy, composer)
            }
        }
    }

    companion object {
        fun get(callback: Any, callbackType: TypeReference?) =
                Inference(callback, callbackType)
    }
}

class InferDecorator(handler: Handling) : DecoratedHandler(handler) {
    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val inference     = getInference(callback, callbackType)
        val inferenceType = if (inference === callback) callbackType else null
        return handler.handle(inference, inferenceType, greedy, composer)
    }

    private fun getInference(
            callback:     Any,
            callbackType: TypeReference?
    ) = when (callback) {
        is InferringCallback -> callback.inferCallback()
        else -> Inference.get(callback, callbackType)
    }
}

