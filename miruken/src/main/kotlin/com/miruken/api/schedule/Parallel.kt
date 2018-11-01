package com.miruken.api.schedule

import com.miruken.callback.Handling
import com.miruken.api.send

class Parallel(
    override val requests: List<Any>,
    override val typeName: String =
            "Miruken.Mediate.Schedule.Parallel,Miruken.Mediate"
) : Scheduled

fun Handling.parallel(requests: List<Any>) =
        send(Parallel(requests))

fun Handling.parallel(vararg requests: Any) =
        send(Parallel(requests.asList()))