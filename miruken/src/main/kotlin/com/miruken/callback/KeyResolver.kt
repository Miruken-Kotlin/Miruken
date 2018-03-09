package com.miruken.callback

import com.miruken.Flags
import com.miruken.TypeFlags

open class KeyResolver : KeyResolving {
    override fun resolve(
            key:      Any,
            flags:    Flags<TypeFlags>,
            handler:  Handling,
            composer: Handling
    ) = when {
        flags has TypeFlags.LAZY ->
                resolveArgumentLazy(key, flags, composer)
        flags has TypeFlags.FUNC ->
            resolveArgumentFunc(key, flags, composer)
        else -> resolveArgument(key, flags, handler, composer)
    }

    open fun resolveKey(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = handler.resolve(key)

    open fun resolveKeyAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAsync(key)

    open fun resolveKeyAll(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAll(key)

    open fun resolveKeyAllAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) = handler.resolveAllAsync(key)

    private fun resolveArgumentLazy(
            key:      Any,
            flags:    Flags<TypeFlags>,
            composer: Handling
    ) =
        lazy(LazyThreadSafetyMode.NONE) {
            // ** MUST ** use composer, composer since
            // handler may be invalidated at this point
            resolveArgument(key, flags, composer, composer)
        }

    private fun resolveArgumentFunc(
            key:      Any,
            flags:    Flags<TypeFlags>,
            composer: Handling
    ): () -> Any? = {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveArgument(key, flags, composer, composer)
    }

    private fun resolveArgument(
            key:      Any,
            flags:    Flags<TypeFlags>,
            handler:  Handling,
            composer: Handling
    ): Any? {
        return when {
            flags has TypeFlags.COLLECTION ||
            flags has TypeFlags.ARRAY ->
                when {
                    flags has TypeFlags.PROMISE ->
                        resolveKeyAllAsync(key, handler, composer)
                    else -> resolveKeyAll(key, handler, composer)
                }
            flags has TypeFlags.PROMISE ->
                resolveKeyAsync(key, handler, composer)
            else -> resolveKey(key, handler, composer)
        }
    }
}