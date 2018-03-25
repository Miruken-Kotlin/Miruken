package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.protocol.proxy
import kotlin.reflect.KType

object ProxyKeyResolver : KeyResolver() {
    override fun validate(key: Any, typeInfo: TypeInfo) {
        val flags = typeInfo.flags
        require(flags has TypeFlags.INTERFACE) {
            "Proxied keys must be interfaces"
        }
        require(!(flags has TypeFlags.ARRAY) &&
                !(flags has TypeFlags.COLLECTION)) {
            "Proxied keys cannot be arrays or collections"
        }
    }

    override fun resolveKey(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling) =
            composer.proxy(key as KType)
}