package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import com.miruken.callback.policy.Policy

@Target(AnnotationTarget.FUNCTION)
@Policy<ProvidesPolicy>(ProvidesPolicy::class)
@Repeatable annotation class Provides

object ProvidesPolicy :
        CovariantPolicy<Provides, Inquiry>(
                { it.key },
                {

                })
