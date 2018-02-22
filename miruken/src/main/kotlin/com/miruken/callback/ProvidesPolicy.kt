package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION,AnnotationTarget.PROPERTY)
@UsePolicy<ProvidesPolicy>(ProvidesPolicy::class)
annotation class Provides

object ProvidesPolicy : CovariantPolicy({
    key { cb: Inquiry -> cb.key } rules {
        matches (callback) returns (key or unit)
        matches () returns key
    }
})
