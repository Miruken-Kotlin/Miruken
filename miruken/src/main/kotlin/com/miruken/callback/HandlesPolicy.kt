package com.miruken.callback

import com.miruken.callback.policy.ContravariantPolicy

@Target(AnnotationTarget.FUNCTION)
@Repeatable annotation class Handles()

object HandlesPolicy :
        ContravariantPolicy<Handles, Command>(
                { it.callback },
                {
                })
