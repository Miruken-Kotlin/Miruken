package com.miruken.callback

import com.miruken.callback.policy.ContravariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<HandlesPolicy>(HandlesPolicy::class)
annotation class Handles

object HandlesPolicy : ContravariantPolicy({
    target({ cb: Command -> cb.callback }, {
        matchCallback(target)
        match(callback)
    })
})
