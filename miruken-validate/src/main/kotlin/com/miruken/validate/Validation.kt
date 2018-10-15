package com.miruken.validate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.ANY_TYPE
import com.miruken.runtime.PROMISE_TYPE
import com.miruken.validate.scopes.Everything
import javax.validation.groups.Default
import kotlin.reflect.KClass
import kotlin.reflect.KType

class Validation(
        val         target:     Any,
        private val targetType: KType? = null,
        vararg      scopes:     KClass<*>
) : Callback, AsyncCallback, DispatchingCallback {
    private var _result: Any? = null
    private val _asyncResults by lazy { mutableListOf<Promise<*>>() }

    val scopes  = scopes.takeIf { it.isNotEmpty() } ?: DEFAULT_SCOPES
    val outcome = ValidationResult.Outcome()
    var stopOnFailure = false

    override var wantsAsync: Boolean = false

    override var isAsync: Boolean = false
        private set

    override val policy get() = ValidatesPolicy

    override fun getCallbackKey() = targetType

    override val resultType: KType?
        get() = if (wantsAsync || isAsync) PROMISE_TYPE else ANY_TYPE

    override var result: Any?
        get() {
            if (_result == null) {
                _result = when (_asyncResults.size) {
                    0 -> null
                    1 -> _asyncResults[0]
                    else -> Promise.all(_asyncResults)
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

    fun satisfiesScopes(vararg scopes: KClass<*>) =
        scopes.contains(Everything::class) ||
        this.scopes.contains(Everything::class) ||
        (scopes.takeIf { it.isNotEmpty() } ?: DEFAULT_SCOPES)
                .all { this.scopes.contains(it) }

    @Suppress("UNUSED_PARAMETER")
    private fun addResult(result: Any, strict: Boolean): Boolean {
        if (result is Promise<*>) {
            _asyncResults.add(result)
            isAsync = true
        }
        _result = null
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ) = policy.dispatch(handler, this, callbackType,
            greedy, composer, ::addResult)
            .then(stopOnFailure && !outcome.isValid) {
                HandleResult.HANDLED_AND_STOP
            }
}

private val DEFAULT_SCOPES = arrayOf(Default::class)