package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import kotlin.reflect.KType

typealias BundleActionBlock      = (Handling) -> Unit
typealias BundleActionAsyncBlock = (Handling) -> Promise<*>
typealias BundleNotifyBlock      = (HandleResult) -> HandleResult

class Bundle(private val all: Boolean = true) :
        AsyncCallback, InferringCallback, DispatchingCallback {

    private val _operations = mutableListOf<Operation>()
    private val _promises   = mutableListOf<Promise<*>>()
    private var _inferring  = false

    private data class Operation(
        val action: BundleActionBlock,
        val notify: BundleNotifyBlock?,
        var result: HandleResult = HandleResult.NOT_HANDLED
    )

    val isEmpty get() = _operations.isEmpty()
    val handled get() = _operations.fold(
            HandleResult.HANDLED) { result, operation ->
        result and operation.result }

    override var wantsAsync: Boolean = false

    override val isAsync: Boolean
        get() = _promises.isNotEmpty()

    override val policy: CallbackPolicy? = null

    fun complete(): Promise<*>? {
        if (isEmpty) {
            return if (wantsAsync) Promise.EMPTY else null
        }
        if (all || _operations.any { it.result.handled }) {
            _operations.forEach {
                if (!it.result.handled)
                    it.notify?.run {
                        it.result = this(it.result)
                    }
            }
        }
        return if (isAsync) {
            Promise.all(_promises) then { Promise.EMPTY }
        } else { if (wantsAsync) Promise.EMPTY else null }
    }

    infix fun add(action: BundleActionBlock) = add(action, null)

    fun add(action: BundleActionBlock,
            notify: BundleNotifyBlock? = null
    ) {
        var act = action
        if (wantsAsync) {
            act = { handler ->
                try {
                    action(handler)
                } catch (e: Throwable) {
                    if (e !is RejectedException) {
                        _promises.add(Promise.reject(e))
                    } else throw e
                }
            }
        }
        _operations.add(Operation(act, notify))
    }

    fun addAsync(action: BundleActionAsyncBlock,
                 notify: BundleNotifyBlock? = null
    ) {
        return add({ handler ->
            _promises.add(action(handler))
        }, notify)
    }

    infix fun addAsync(action: BundleActionAsyncBlock) =
            addAsync(action, null)

    override fun inferCallback(): Any {
        return if (_inferring) this
        else Bundle(all).also {
            it._operations.addAll(_operations)
            it.wantsAsync = wantsAsync
            it._inferring = true
        }
    }

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        if (_operations.isEmpty())
            return HandleResult.NOT_HANDLED

        var proxy : Handling = ProxyHandler(handler, composer)
        if (_inferring) proxy = proxy.infer

        var handled = HandleResult.HANDLED
        for (operation in _operations) {
            if (all || greedy) {
                var opResult = operation.result
                if (!opResult.handled || greedy) {
                    dispatch(proxy, operation) success {
                        val result = operation.notify?.invoke(this) ?: this
                        operation.result = operation.result or result
                        opResult = operation.result or opResult
                    }
                }
                handled = when (all) {
                    true -> opResult and handled
                    else -> opResult or  handled
                }
                if (handled.stop) break
            } else {
                dispatch(proxy, operation) success {
                    val result = operation.notify?.invoke(this) ?: this
                    operation.result = operation.result or result
                }
                handled = operation.result
                if (handled.handled || handled.stop) break
            }
        }
        return handled
    }

    private fun dispatch(
            proxy:     Handling,
            operation: Operation
    ) : HandleResult {
        return try {
            operation.action(proxy)
            HandleResult.HANDLED
        } catch (e: RejectedException) {
            HandleResult.NOT_HANDLED
        }
    }

    private class ProxyHandler(
            handler:      Any,
            val composer: Handling
    ) : HandlerAdapter(handler) {
        override fun handleCallback(
                callback:     Any,
                callbackType: KType?,
                greedy:       Boolean,
                composer:     Handling
        ): HandleResult {
            if (callback is Composition)
                return HandleResult.NOT_HANDLED
            val result = super.handleCallback(callback,
                    callbackType, greedy, composer + this.composer)
            return result failure {
                throw RejectedException(callback)
            } ?: result
        }
    }
}