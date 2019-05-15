package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.concurrent.all
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class Command(
        val callback:     Any,
        val callbackType: TypeReference?,
        val many:         Boolean = false
) : Callback, AsyncCallback,
        FilteringCallback, BatchingCallback,
        DispatchingCallback {

    private var _result: Any? = null
    private val _promises     = mutableListOf<Promise<*>>()
    private val _results      = mutableListOf<Any>()

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override val canFilter: Boolean
        get() = (callback as? FilteringCallback)?.canFilter != false

    override val canBatch: Boolean
        get() = (callback as? BatchingCallback)?.canBatch != false

    override fun getCallbackKey() = callbackType?.kotlinType

    override var policy: CallbackPolicy = HandlesPolicy

    val results: List<Any> get() = _results

    override val resultType: KType?
        get() {
            var resultType = TypeReference.ANY_TYPE
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
                _result = if (isAsync) {
                    Promise.all(_promises) then {
                        if (many) _results else _results.firstOrNull()
                    }
                } else {
                    if (many) _result else _results.firstOrNull()
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
        val accepted = include(response)
        if (accepted) _result = null
        return accepted
    }

    private fun include(resolution: Any): Boolean {
        val res = (resolution as? Promise<*>)
                ?.takeIf { it.state == PromiseState.FULFILLED }
                ?.let { it.get() } ?: resolution

        if (res is Promise<*>) {
            isAsync = true
            _promises.add(res.then {
                if (it != null) _results.add(it)
            })
        } else {
            _results.add(res)
        }
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val size = _results.size
        return policy.dispatch(handler, this,
                this.callbackType ?: callbackType,
                greedy, composer, ::respond).otherwise(
                _results.size > size)
    }
}