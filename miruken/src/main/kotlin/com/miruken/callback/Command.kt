package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

open class Command(
        val callback: Any,
        callbackType: KType?  = null,
        val many:     Boolean = false
) : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null
    private val _results = mutableListOf<Any>()
    private var _policy: CallbackPolicy? = null
    private val _callbackType by lazy {
        callbackType ?: callback::class.let {
            it.createType(it.typeParameters.map {
                KTypeProjection.STAR })
        }
    }

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override var policy: CallbackPolicy?
        get() = _policy ?: HandlesPolicy
        set(value) { _policy = value }

    val results: List<Any> get() = _results.toList()

    override val resultType: KType?
        get() = if (wantsAsync || isAsync)
            Promise::class.createType(listOf(
                    KTypeProjection.invariant(_callbackType)))
            else _callbackType

    override var result: Any?
        get() = {
            if (_result != null) {
                _result
            } else if (!many) {
                if (_results.isNotEmpty()) {
                    _result = _results.first()
                }
            } else if (isAsync) {
                _result = Promise.all(_results
                        .map { Promise.resolve(it) })
            } else {
                _result = _results
            }
            if (wantsAsync && !isAsync) {
                _result = Promise.resolve(_result ?: Unit)
            }
            _result
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
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val size = _results.size
        return policy!!.dispatch(handler, this, greedy,
                composer, ::respond).otherwise(
                _results.size > size)
    }
}