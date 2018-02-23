package com.miruken.callback.policy

import com.miruken.callback.Handling

interface ArgumentResolving {
    fun validate(argument: Argument) {}

    fun resolve(
            argument: Argument,
            handler:  Handling,
            composer: Handling
    ): Any?
}
