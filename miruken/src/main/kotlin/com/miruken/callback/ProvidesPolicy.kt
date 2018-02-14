package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<ProvidesPolicy>(ProvidesPolicy::class)
@Repeatable annotation class Provides

object ProvidesPolicy :
        CovariantPolicy<Inquiry>({ it.key },
                {

                })
