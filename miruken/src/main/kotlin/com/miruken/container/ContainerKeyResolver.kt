package com.miruken.container

import com.miruken.TypeInfo
import com.miruken.callback.KeyResolver
import com.miruken.callback.Handling
import com.miruken.callback.Inquiry

object ContainerKeyResolver : KeyResolver() {
    override fun resolveKey(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry?
    ) = Container(handler).resolve(key)

    override  fun resolveKeyAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry?
    ) = Container(handler).resolveAsync(key)

    override fun resolveKeyAll(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry?
    ) = Container(handler).resolveAll(key)

    override fun resolveKeyAllAsync(
            key:      Any,
            typeInfo: TypeInfo,
            handler:  Handling,
            composer: Handling,
            parent:   Inquiry?
    ) = Container(handler).resolveAllAsync(key)
}