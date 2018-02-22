package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

open class Inquiry(val key: Any, val many: Boolean = false)
    : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null
    private val _resolutions  = mutableListOf<Any>()
    private val _keyType: KType by lazy {
        when (key) {
            is KType -> key
            is KClass<*> -> key.createType(
                    key.typeParameters.map { STAR })
            else -> Any::class.starProjectedType
        }
    }
    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    final override val policy: CallbackPolicy? = ProvidesPolicy

    val resolutions: List<Any> get() = _resolutions.toList()

    override val resultType: KType?
        get() = _keyType.let {
            var resultType = it
            if (many) {
                resultType = List::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            if (wantsAsync || isAsync) {
                resultType = Promise::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            resultType
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
                            } else { it }
                        }
                    } else { result }
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
        val result = if (implied(handler, greedy, composer))
            HandleResult.HANDLED else HandleResult.HANDLED
        if (result.handled && !greedy) return result

        val count = _resolutions.size
        return result then {
            policy!!.dispatch(handler, this, greedy, composer)
        } then {
            if (_resolutions.size > count) HandleResult.HANDLED
            else HandleResult.NOT_HANDLED
        }
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

