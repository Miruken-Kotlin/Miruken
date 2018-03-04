package com.miruken.error

import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.runtime.typeOf

interface Errors {
    fun handleException(
            exception: Throwable,
            callback:   Any? = null,
            context:    Any? = null
    ): Promise<*>

    companion object {
        val PROTOCOL = typeOf<Errors>()
        operator fun invoke(adapter: ProtocolAdapter) =
                Protocol.proxy(adapter, PROTOCOL) as Errors
    }
}