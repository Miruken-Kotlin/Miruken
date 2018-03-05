package com.miruken.callback

import com.miruken.callback.policy.Argument
import com.miruken.callback.policy.ArgumentResolving
import com.miruken.callback.policy.TypeFlags
import com.miruken.concurrent.Promise

open class ArgumentResolver : ArgumentResolving {
    override fun resolve(
            argument: Argument,
            handler:  Handling,
            composer: Handling
    ) = when {
        argument.flags has TypeFlags.LAZY ->
                resolveArgumentLazy(argument, composer)
        argument.flags has TypeFlags.FUNC ->
            resolveArgumentFunc(argument, composer)
        else -> resolveArgument(argument, handler, composer)
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
            argument: Argument,
            composer: Handling
    ) =
        lazy(LazyThreadSafetyMode.NONE) {
            // ** MUST ** use composer, composer since
            // handler may be invalidated at this point
            resolveArgument(argument, composer, composer)
        }

    private fun resolveArgumentFunc(
            argument: Argument,
            composer: Handling
    ): () -> Any? = {
        // ** MUST ** use composer, composer since
        // handler may be invalidated at this point
        resolveArgument(argument, composer, composer)
    }

    private fun resolveArgument(
            argument: Argument,
            handler:  Handling,
            composer: Handling
    ): Any? {
        val key   = argument.key ?: return null
        val flags = argument.flags

        return when {
            flags has TypeFlags.COLLECTION ->
                when {
                    flags has TypeFlags.PROMISE -> {
                        resolveKeyAllAsync(key, handler, composer)
                    }
                    else -> resolveKeyAll(key, handler, composer)
                }
            flags has TypeFlags.PROMISE -> {
                resolveKeyAsync(key, handler, composer)
            }
            else -> resolveKey(key, handler, composer)
        }
    }
}