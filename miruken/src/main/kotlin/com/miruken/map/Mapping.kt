package com.miruken.map

import com.miruken.callback.*
import com.miruken.callback.policy.CallbackPolicy
import com.miruken.concurrent.Promise
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class Mapping(
        val source:     Any,
        val targetType: KType,
        val target:     Any? = null,
        val format:     Any? = null
) : Callback, AsyncCallback, DispatchingCallback {

    private var _result: Any? = null

    override var wantsAsync: Boolean = false

    override var isAsync: Boolean = false
        private set

    override val policy: CallbackPolicy? = MapsPolicy

    override val resultType: KType? = targetType
            .takeIf { !wantsAsync && !isAsync }
            ?: Promise::class.createType(listOf(
                    KTypeProjection.invariant(targetType)))

    override var result: Any?
        get() {
            if (wantsAsync && !isAsync)
                _result = Promise.resolve(_result)
            return _result
        }
        set(value) {
            _result = value
            isAsync = _result is Promise<*>
        }

    @Suppress("UNUSED_PARAMETER")
    fun mapped(mapping: Any, strict: Boolean) : Boolean {
        result = mapping
        return true
    }

    override fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: Handling
    ): HandleResult =
        policy!!.dispatch(handler, this, greedy,
            composer, ::mapped).otherwise(_result != null)
}
