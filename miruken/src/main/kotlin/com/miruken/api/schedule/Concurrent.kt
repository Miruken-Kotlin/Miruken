package com.miruken.api.schedule

import com.miruken.callback.Handling
import com.miruken.api.send

data class Concurrent(
        override val requests: List<Any>,
        override val typeName: String =
                "Miruken.Mediate.Schedule.Concurrent,Miruken.Mediate"
) : Scheduled

fun Handling.concurrent(requests: List<Any>) =
        send(Concurrent(requests))

fun Handling.concurrent(vararg requests: Any) =
        send(Concurrent(requests.asList()))