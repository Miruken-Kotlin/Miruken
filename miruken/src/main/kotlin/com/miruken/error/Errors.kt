package com.miruken.error

import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface Errors {
    fun handleException(
            exception: Throwable,
            callback:   Any? = null,
            context:    Any? = null
    ): Promise<*>

    companion object {
        val PROTOCOL = typeOf<Errors>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Errors
    }
}