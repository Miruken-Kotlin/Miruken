package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy
import com.miruken.callback.policy.UsePolicy
import com.miruken.callback.policy.orUnit
import com.miruken.runtime.getKType

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<ProvidesPolicy>(ProvidesPolicy::class)
annotation class Provides

object ProvidesPolicy : CovariantPolicy({
    key({ cb: Inquiry -> cb.key }, {
        match(returnKey.orUnit, callback)
        match(returnKey)
    })
})
