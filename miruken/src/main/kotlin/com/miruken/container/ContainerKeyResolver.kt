package com.miruken.container

import com.miruken.callback.KeyResolver
import com.miruken.callback.Handling

object ContainerKeyResolver : KeyResolver() {
    override fun resolveKey(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = Container(handler).resolve(key)

    override  fun resolveKeyAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = Container(handler).resolveAsync(key)

    override fun resolveKeyAll(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = Container(handler).resolveAll(key)

    override fun resolveKeyAllAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = Container(handler).resolveAllAsync(key)
}