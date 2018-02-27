package com.miruken.callback

import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.ANY_STAR
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

open class Inquiry(val key: Any, val many: Boolean = false)
    : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null
    private val _promises     = mutableListOf<Promise<*>>()
    private val _resolutions  = mutableListOf<Any>()
    private val _keyType: KType by lazy {
        when (key) {
            is KType -> key
            is KClass<*> -> key.starProjectedType
            else -> ANY_STAR
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
            return resultType
        }

    override var result: Any?
        get() {
            if (_result != null) return _result
            _result = if (isAsync) {
                Promise.all(_promises) then {
                    val flat = flatten(_resolutions, it)
                    if (many) flat else flat.firstOrNull()
                }
            } else {
                val flat = flatten(_resolutions)
                if (many) flat else flat.firstOrNull()
            }
            if (wantsAsync && _result !is Promise<*>) {
                _result = Promise.resolve(_result)
            }
            return _result
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
            !strict && resolution is Collection<*> ->
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
        if (resolution is Promise<*>) {
            isAsync = true
            _promises.add(resolution.then {
                it?.takeIf { isSatisfied(it, greedy, composer) }
            })
        } else if (!isSatisfied(resolution, greedy, composer)) {
            return false
        } else {
            _resolutions.add(resolution)
        }
        return true
    }

    protected open fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean = true

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult {
        val result = if (implied(handler, greedy, composer))
            HandleResult.HANDLED else HandleResult.NOT_HANDLED
        if (result.handled && !greedy) return result

        val count = _resolutions.size + _promises.size
        return result then {
            policy!!.dispatch(handler, this@Inquiry, greedy, composer)
            { r, strict -> resolve(r, strict, greedy, composer) }
        } then {
            if (_resolutions.size + _promises.size > count)
                HandleResult.HANDLED else HandleResult.NOT_HANDLED
        }
    }

    private fun implied(
            item:      Any,
            greedy:    Boolean,
            composer:  Handling
    ) = isAssignableTo(key, item) &&
                resolve(item, false, greedy, composer)

    private fun flatten(vararg lists: List<*>): List<Any> {
        val flat = mutableSetOf<Any>()
        lists.flatMap { it }
             .forEach {
                 if (it is Iterable<*>)
                     flat.addAll(it.filterNotNull())
                 else if (it != null)
                     flat.add(it)
             }
        return flat.toList()
    }
}

