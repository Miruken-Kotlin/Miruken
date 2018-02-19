package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.PROMISE_TYPE
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.createType

open class Inquiry(val key: Any, val many: Boolean = false)
    : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null
    private val _resolutions  = mutableListOf<Any>()
    private val _keyType: KType? by lazy {
        when (key) {
            is KType -> key
            is KClass<*> -> key.createType(
                    key.typeParameters.map { STAR })
            else -> null
        }
    }
    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override val policy: CallbackPolicy? = ProvidesPolicy

    val resolutions: List<Any> get() = _resolutions.toList()

    override val resultType: KType?
        get() = when (key) {
            is KType,
            is KClass<*> -> if (wantsAsync || isAsync)
                Promise::class.createType(
                        listOf(invariant(_keyType!!)))
                else _keyType
            else -> if (wantsAsync || isAsync)
                PROMISE_TYPE else null
        }

    override var result: Any?
        get() = {
            if (_result != null) {
                _result
            } else if (!many) {
                if (_resolutions.isNotEmpty()) {
                    val result = _resolutions.first()
                    _result = if (result is Promise<*>) {
                        result.then {
                            if (it is Collection<*>) {
                                it.firstOrNull()
                            } else {
                                it
                            }
                        }
                    } else {
                        result
                    }
                }
            } else if (isAsync) {
                _result = Promise.all(_resolutions
                        .map { Promise.resolve(it) })
                        .then(::flatten)
            } else {
                _result = flatten(_resolutions)
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

    fun resolve(resolution: Any, composer: Handling) =
            resolve(resolution, false, false, composer)

    fun resolve(
            resolution: Any,
            strict:     Boolean,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        val resolved = when {
            strict && resolution is Collection<*> ->
                resolution.filterNotNull().fold(false, { s, res ->
                    include(res, greedy, composer) || s
                })
            else -> include(resolution, greedy, composer)
        }
        if (resolved) _result = null
        return resolved
    }

    private fun include(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        if ((!many && _resolutions.isNotEmpty()))
            return false

        var res = resolution
        var promise = res as? Promise<*>
        if (promise != null) {
            isAsync = true
            if (many) promise = promise.catch {}
            res = promise.then {
                it?.takeIf { isSatisfied(it, greedy, composer) }
                        ?.let { result }
            }
        } else if (!isSatisfied(res, greedy, composer))
            return false

        _resolutions.add(res)
        return true
    }

    protected open fun isSatisfied(
            resolution: Any,
            greedy: Boolean,
            composer: Handling
    ): Boolean = true

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        TODO("not implemented")
    }

    private fun implied(
            item:      Any,
            greedy:    Boolean,
            composer:  Handling
    ): Boolean {
        return isAssignableTo(key, item) &&
                resolve(item, false, greedy, composer)
    }

    private fun flatten(list: List<Any?>): List<Any?> {
        return list.flatMap {
            when (it) {
                is Iterable<Any?> -> it
                else -> listOf(it)
            }
        }.distinct()
    }
}

