package com.miruken.map

import com.miruken.callback.policy.BivariantPolicy
import com.miruken.callback.policy.UsePolicy

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<MapsPolicy>(MapsPolicy::class)
annotation class Handles

object MapsPolicy : BivariantPolicy({
    key { cb: Mapping -> cb.targetType } target { it.source } rules {
        matches (target) returns key
        matches (target, callback) returns key
        matches (callback) returns (key or unit)
    }
})