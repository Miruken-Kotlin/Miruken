package com.miruken.callback

import com.miruken.callback.policy.ContravariantPolicy
import com.miruken.callback.policy.Policy

@Target(AnnotationTarget.FUNCTION)
@Policy<HandlesPolicy>(HandlesPolicy::class)
@Repeatable annotation class Handles

object HandlesPolicy :
        ContravariantPolicy<Handles, Command>(
                { it.callback },
                {
                })
