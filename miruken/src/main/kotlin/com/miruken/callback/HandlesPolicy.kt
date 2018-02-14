package com.miruken.callback

import com.miruken.callback.policy.ContravariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<HandlesPolicy>(HandlesPolicy::class)
@Repeatable annotation class Handles

object HandlesPolicy :
        ContravariantPolicy<Command>({ it.callback },
                {
                })
