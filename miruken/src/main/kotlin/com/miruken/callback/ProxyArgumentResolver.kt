package com.miruken.callback

import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.TypeFlags
import com.miruken.protocol.proxy
import kotlin.reflect.KType

object ProxyArgumentResolver : ArgumentResolver() {
    override fun validate(argument: Argument) {
        require(argument.flags has TypeFlags.INTERFACE) {
            "Proxy parameters must be interfaces"
        }
        require(!(argument.flags has TypeFlags.ARRAY) &&
                !(argument.flags has TypeFlags.COLLECTION)) {
            "Proxy parameters cannot be arrays or collections"
        }
    }

    override fun resolveKey(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = composer.proxy(key as KType)
}