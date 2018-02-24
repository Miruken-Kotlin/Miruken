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
    ): Any? {
        return when {
            argument.flags has TypeFlags.LAZY ->
                    resolveArgumentLazy(argument, handler, composer)
            else -> resolveArgument(argument, handler, composer)
        }
    }

    open fun resolveKey(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) : Any? = handler.resolve(key)

    open fun resolveKeyAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) : Promise<Any?> = handler.resolveAsync(key)

    open fun resolveKeyAll(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) : List<Any> = handler.resolveAll(key)

    open fun resolveKeyAllAsync(
            key:      Any,
            handler:  Handling,
            composer: Handling
    ) : Promise<List<Any>> = handler.resolveAllAsync(key)

    private fun resolveArgumentLazy(
            argument: Argument,
            handler:  Handling,
            composer: Handling
    ): () -> Any? = { resolveArgument(argument, handler, composer) }

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