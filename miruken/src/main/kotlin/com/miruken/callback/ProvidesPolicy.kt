package com.miruken.callback

import com.miruken.callback.policy.CovariantPolicy

@Target(AnnotationTarget.FUNCTION)
@Repeatable annotation class Provides

object ProvidesPolicy : CovariantPolicy<Provides>({
})
