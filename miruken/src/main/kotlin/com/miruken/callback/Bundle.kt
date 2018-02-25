package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all

typealias BundleActionBlock = (Handling) -> Any?
typealias BundleNotifyBlock = (HandleResult) -> HandleResult

class Bundle(private val all: Boolean = true) :
        AsyncCallback, ResolvingCallback, DispatchingCallback {

    private val _operations = mutableListOf<Operation>()
    private val _promises   = mutableListOf<Promise<*>>()
    private var _resolving  = false

    private data class Operation(
        val action: BundleActionBlock,
        val notify: BundleNotifyBlock?,
        var result: HandleResult = HandleResult.NOT_HANDLED
    )

    val isEmpty get() = _operations.isEmpty()
    val handled get() = _operations.fold(
            HandleResult.NOT_HANDLED, { result, operation ->
                result or operation.result })

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

    fun add(action: BundleActionBlock,
            notify: BundleNotifyBlock? = null
    ) : Bundle {
        if (wantsAsync) {
            val act: BundleActionBlock = { handler ->
                try {
                    action(handler)?.run {
                        if (this is Promise<*>)
                            _promises.add(this)
                    }
                } catch (e: Throwable) {
                    if (e !is RejectedException) {
                        _promises.add(Promise.reject(e))
                    } else throw e
                }
            }
            _operations.add(Operation(act, notify))
        }
        return this
    }

    infix operator fun plus(action: BundleActionBlock) =
            add(action)

    override fun getResolveCallback(): Any {
        return if (_resolving) this
        else Bundle(all).also {
            it._operations.addAll(_operations)
            it._resolving = true
        }
    }

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        if (_operations.isEmpty())
            return HandleResult.NOT_HANDLED

        var proxy : Handling = ProxyHandler(handler, composer)
        if (_resolving) proxy = proxy.resolving()

        var handled = HandleResult.HANDLED
        for (operation in _operations) {
            if (all || greedy) {
                var opResult = operation.result
                if (!opResult.handled || greedy) {
                    dispatch(proxy, operation).success({
                        operation.notify?.let { it(this) } ?: this
                    })?.let {
                        opResult = opResult or it
                        operation.result = opResult
                        opResult
                    }
                }
                handled = when (all) {
                    true -> opResult and handled
                    else -> opResult or  handled
                }
                if (handled.stop) break
            } else {
                dispatch(proxy, operation).success({
                    operation.notify?.let { it(this) } ?: this
                })?.let {
                    operation.result = operation.result or it
                    handled = it
                }
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
                callback: Any,
                greedy:   Boolean,
                composer: Handling
        ): HandleResult {
            if (callback is Composition)
                return HandleResult.NOT_HANDLED
            val result = super.handleCallback(callback, greedy,
                    composer + this.composer)
            return result failure {
                throw RejectedException(callback)
            } ?: result
        }
    }
}