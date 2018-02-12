package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Repeatable annotation class Provides

object ProvidesPolicy :
        CovariantPolicy<Provides, Inquiry>(
                { it.key },
                {

                })
