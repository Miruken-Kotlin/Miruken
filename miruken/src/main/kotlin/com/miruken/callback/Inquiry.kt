package com.miruken.callback

import kotlin.reflect.KClass

open class Inquiry(val key: Any, many: Boolean = false)
    : ICallback, IAsyncCallback, IDispatchCallback {

    private val _resolutions = mutableListOf<Any>()
    private var _result = null

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override val policy: CallbackPolicy? = null

    val resolutions: List<Any> = _resolutions.toList()

    override val resultType: KClass<*>?
        get() = if (wantsAsync || isAsync) Any::class else null

    override var result: Any?
        get() = TODO("not implemented")
        set(value) {}

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: IHandler
    ): HandleResult {
        TODO("not implemented")
    }
}