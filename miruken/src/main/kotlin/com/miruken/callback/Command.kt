package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.ANY_TYPE
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class Command(
        val callback:     Any,
        val callbackType: KType?,
        val many:         Boolean = false
) : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null
    private val _results = mutableListOf<Any>()
    private var _policy: CallbackPolicy? = null

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override fun getCallbackKey() = callbackType

    override var policy: CallbackPolicy?
        get() = _policy ?: HandlesPolicy
        set(value) { _policy = value }

    val results: List<Any> get() = _results

    override val resultType: KType?
        get() {
            var resultType = ANY_TYPE
            if (many) {
                resultType = List::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            if (wantsAsync || isAsync) {
                resultType = Promise::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            return resultType
        }

    override var result: Any?
        get() {
            if (_result == null) {
                if (!many) {
                    if (_results.isNotEmpty()) {
                        _result = _results.first()
                    }
                } else if (isAsync) {
                    _result = Promise.all(_results
                            .map { Promise.resolve(it) })
                } else {
                    _result = _results
                }
            }
            if (isAsync) {
                if (!wantsAsync) {
                    _result = (_result as? Promise<*>)?.get()
                }
            } else if (wantsAsync) {
                _result = _result?.let { Promise.resolve(it) }
                        ?: Promise.EMPTY
            }
            return _result
        }
        set(value) {
            _result = value
            isAsync = _result is Promise<*>
        }

    @Suppress("UNUSED_PARAMETER")
    fun respond(response: Any, strict: Boolean) : Boolean {
        if ((!many && _results.isNotEmpty())) return false
        if (response is Promise<*>) isAsync = true
        _results.add(response)
        _result = null
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val size = _results.size
        return policy!!.dispatch(handler, this,
                this.callbackType ?: callbackType,
                greedy, composer, ::respond).otherwise(
                _results.size > size)
    }
}