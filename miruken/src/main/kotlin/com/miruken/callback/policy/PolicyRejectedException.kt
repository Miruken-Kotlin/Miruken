package com.miruken.callback.policy

import kotlin.reflect.KCallable

class PolicyRejectedException(
        val policy:  CallbackPolicy,
        val culprit: KCallable<*>,
        message:     String
) : Exception(message)