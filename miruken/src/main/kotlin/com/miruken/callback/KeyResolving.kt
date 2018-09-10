package com.miruken.callback

import com.miruken.TypeInfo

interface KeyResolving {
    fun validate(key: Any, typeInfo: TypeInfo) {}

    fun resolve(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry? = null
    ): Any?
}
