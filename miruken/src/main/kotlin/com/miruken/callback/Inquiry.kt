package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import kotlin.reflect.KClass

open class Inquiry(val key: Any, val many: Boolean = false)
    : Callback, AsyncCallback, Dispatching {

    private val _resolutions = mutableListOf<Any>()
    private var _result: Any? = null

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override val policy: CallbackPolicy? = null

    val resolutions: List<Any> get() = _resolutions.toList()

    override val resultType: KClass<*>?
        get() = if (wantsAsync || isAsync) Promise::class else null

    override var result: Any?
        get() = {
            if (_result != null) {
                _result
            } else if (!many) {
                if (_resolutions.isNotEmpty()) {
                    val result = _resolutions.first()
                    _result = if (result is Promise<*>) {
                        result.then {
                            if (it is List<*>) {
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
                        .then { flatten(it) }
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

    fun resolve(
            resolution: Any,
            composer: Handling
    ): Boolean
    {
        return resolve(resolution, false, false, composer)
    }

    fun resolve(
            resolution: Any,
            strict:     Boolean,
            greedy:     Boolean,
            composer:   Handling
    ) : Boolean {
        val resolved =
                if (strict && resolution is List<*>) {
                    resolution.filterNotNull()
                              .fold(false, {s, res ->
                        include(res, greedy, composer) || s
                    })
                } else {
                    include(resolution, greedy, composer)
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
            promise = promise.then {
                if (it != null && isSatisfied(it, greedy, composer)) {
                    result
                } else { null }
            }
            res = promise
        } else if (!isSatisfied(res, greedy, composer))
            return false

        _resolutions.add(res)
        return true
    }

    open fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
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
            invariant: Boolean,
            greedy:    Boolean,
            composer:  Handling
    ) : Boolean {
        val type       = key as? KClass<*> ?: return false
        val compatible = if (invariant) {
            type == item::class
        } else {
            type.isInstance(item)
        }
        return compatible && resolve(item, false, greedy, composer)
    }
}

private fun flatten(list: List<Any?>) : List<Any?> {
    return list.flatMap {
        when (it) {
            is Iterable<Any?> -> it
            else -> listOf(it)
        }
    }.distinct()
}