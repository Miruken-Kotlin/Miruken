package com.miruken.mediate.schedule

import com.miruken.callback.Handling
import com.miruken.mediate.send

class Sequential (
    override val requests: List<Any>
) : Scheduled

fun Handling.sequential(requests: List<Any>) =
        send(Sequential(requests))

fun Handling.sequential(vararg requests: Any) =
        send(Sequential(requests.asList()))