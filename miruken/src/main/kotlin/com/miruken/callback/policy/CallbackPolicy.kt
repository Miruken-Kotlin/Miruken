package com.miruken.callback.policy

import com.miruken.callback.FilteringProvider
import com.miruken.callback.HandleResult
import com.miruken.callback.Handling
import kotlin.reflect.KCallable

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy : Comparator<Any> {
    private val _rules   = mutableListOf<MethodRule>()
    private val _filters = mutableListOf<FilteringProvider>()

    fun addRule(vararg rules: MethodRule) =
            _rules.addAll(rules)

    fun addFilters(vararg filters: FilteringProvider) =
            _filters.addAll(filters)

    fun match(method: MethodDispatch) =
            _rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(
            bindingInfo: PolicyMethodBindingInfo
    ): PolicyMethodBinding =
            PolicyMethodBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMethodBindingInfo): Any? =
            bindingInfo.inKey ?: bindingInfo.outKey

    abstract fun getKey(callback: Any) : Any?

    abstract fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any>

    open fun approve(callback: Any, annotation: Annotation) : Boolean {
        return true
    }

    fun dispatch(
            handler:  Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ): HandleResult {
        TODO("not implemented")
    }
}