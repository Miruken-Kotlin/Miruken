package com.miruken.callback

import com.miruken.Flags

interface KeyResolving {
    fun validate(key: Any, flags: Flags<TypeFlags>) {
    }

    fun resolve(
            key:      Any,
            flags:    Flags<TypeFlags>,
            handler:  Handling,
            composer: Handling
    ): Any?
}
