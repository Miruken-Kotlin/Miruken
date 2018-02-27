package com.miruken.callback

import com.miruken.protocol.ProtocolAdapter
import java.lang.reflect.Method
import kotlin.reflect.KType

@FunctionalInterface
interface Handling : ProtocolAdapter {
    fun handle(
            callback: Any,
            greedy:   Boolean   = false,
            composer: Handling? = null
    ) : HandleResult

    override fun dispatch(
            protocol: KType,
            method:   Method,
            args:     Array<Any?>
    ): Any? {
        return null
    }
}