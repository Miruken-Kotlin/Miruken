package com.miruken.api.schedule

import com.miruken.callback.Handling
import com.miruken.api.send

class Sequential (
    override val requests: List<Any>,
    override val typeName: String =
            "Miruken.Mediate.Schedule.Sequential,Miruken.Mediate"
) : Scheduled

fun Handling.sequential(requests: List<Any>) =
        send(Sequential(requests))

fun Handling.sequential(vararg requests: Any) =
        send(Sequential(requests.asList()))