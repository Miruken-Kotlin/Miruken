package com.miruken.callback

import com.miruken.TypeInfo

interface KeyResolving {
    fun validate(key: Any, typeInfo: TypeInfo) {}

    fun resolve(
            inquiry:  Inquiry,
            typeInfo: TypeInfo,
            handler:  Handling
    ): Any?
}
