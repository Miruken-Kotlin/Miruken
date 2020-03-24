package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.concurrent.all
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

open class Creation(
            type:   Any,
        val many:   Boolean = false
) : Callback, AsyncCallback, DispatchingCallback {
    private var _result: Any? = null
    private val _promises     = mutableListOf<Promise<*>>()
    private val _instances    = mutableListOf<Any>()

    val type = when (type) {
        is KType -> type
        is KClass<*> -> type.starProjectedType
        is TypeReference -> type.kotlinType
        else -> error("Only types can be created")
    }

    var target: Any? = null
        private set

    final override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    final override val policy get() = CreatesPolicy

    val instances: List<Any> get() = _instances.toList()

    override val resultType: KType?
        get() = type.let {
            var resultType = it
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
                        if (many) _instances else _instances.firstOrNull()
                    }
                } else {
                    if (many) _instances else _instances.firstOrNull()
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
    fun addInstance(
            instance: Any,
            strict:   Boolean
    ): Boolean {
        val res = (instance as? Promise<*>)
                ?.takeIf { it.state == PromiseState.FULFILLED }
                ?.get() ?: instance

        if (res is Promise<*>) {
            isAsync = true
            _promises.add(res.then { i ->
                if (i != null) _instances.add(i)
            } catch {
                // ignore failures
            })
        } else {
            _instances.add(res)
        }

        _result = null
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        val count = _instances.size + _promises.size
        return policy.dispatch(handler, this, callbackType,
                greedy, composer, ::addInstance).otherwise(
                _instances.size + _promises.size > count)
    }
}