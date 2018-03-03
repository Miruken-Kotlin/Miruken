package com.miruken.map

import com.miruken.callback.policy.BivariantPolicy
import com.miruken.callback.policy.CallbackPolicyBuilder
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<MapsPolicy>(MapsPolicy::class)
annotation class Handles

class MapsPolicy : BivariantPolicy({
    CallbackPolicyBuilder.Completed
})