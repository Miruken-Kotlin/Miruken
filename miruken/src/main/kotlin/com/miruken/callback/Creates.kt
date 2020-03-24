package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@UsePolicy(CreatesPolicy::class)
annotation class Creates

object CreatesPolicy : CovariantPolicy({
    key { cb: Creation -> cb.type } rules {
        matches (callback) returns (key or unit)
        matches () returns key
    }
})