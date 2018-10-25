package com.miruken.mediate.schedule

import com.miruken.callback.Handling
import com.miruken.mediate.send

class Parallel(
    override val requests: List<Any>
) : Scheduled

fun Handling.parallel(requests: List<Any>) =
        send(Parallel(requests))

fun Handling.parallel(vararg requests: Any) =
        send(Parallel(requests.asList()))