package com.miruken.validate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.ANY_TYPE
import com.miruken.runtime.PROMISE_TYPE
import kotlin.reflect.KType

class Validation(
        val target: Any,
        private val targetType: KType,
        scope: Any? = null
) : Callback, AsyncCallback, DispatchingCallback {
    private var _result: Any? = null
    private val _asyncResults = mutableListOf<Promise<*>>()

    val outcome       = ValidationResult.Outcome()
    val scopeMatcher  = createScopeMatcher(scope)
    var stopOnFailure = false

    override var wantsAsync: Boolean = false

    override var isAsync: Boolean = false
        private set

    override val policy get() = ValidatesPolicy

    override val resultType: KType?
        get() = if (wantsAsync || isAsync) PROMISE_TYPE else ANY_TYPE

    override var result: Any?
        get() {
            if (_result != null) return _result
            _result = when (_asyncResults.size) {
                0 -> null
                1 -> _asyncResults[0]
                else -> Promise.all(_asyncResults)
            }
            if (wantsAsync && !isAsync) {
                _result = _result?.let { Promise.resolve(it) }
                        ?: Promise.EMPTY
            }
            return _result
        }
        set(value) {
            _result = value
            isAsync = _result is Promise<*>
        }

    override fun getCallbackKey() = targetType

    @Suppress("UNUSED_PARAMETER")
    fun addResult(result: Any, strict: Boolean) : Boolean {
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

    private fun createScopeMatcher(scope: Any?): ScopeMatching =
            when (scope) {
                null -> EqualsScopeMatcher.DEFAULT
                is Collection<*> ->  when (scope.size) {
                    0 -> EqualsScopeMatcher.DEFAULT
                    1 -> scope.first().let {
                        it as? ScopeMatching ?: EqualsScopeMatcher(it)
                    }
                    else -> CompositeScopeMatcher(scope.map {
                        createScopeMatcher(arrayOf(it))
                    })
                }
                is Array<*> ->  when (scope.size) {
                    0 -> EqualsScopeMatcher.DEFAULT
                    1 -> scope[0].let {
                        it as? ScopeMatching ?: EqualsScopeMatcher(it)
                    }
                    else -> CompositeScopeMatcher(scope.map {
                        createScopeMatcher(arrayOf(it))
                    })
                }
                else -> EqualsScopeMatcher(scope)
            }
}