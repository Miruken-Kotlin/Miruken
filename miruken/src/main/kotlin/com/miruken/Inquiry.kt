package com.miruken

import kotlin.reflect.KType

open class Inquiry(val key: Any, many: Boolean = false)
    : ICallback, IAsyncCallback, IDispatchCallback {

    private val _resolutions = mutableListOf<Any>()
    private var _result = null

    final override var isAsync: Boolean = false
        private set

    override val policy: CallbackPolicy? = null

    val resolutions: List<Any>
        get() = _resolutions.toList()

    override val resultType: KType?
        get() = if (wantsAsync || isAsync) Deferred else null
}