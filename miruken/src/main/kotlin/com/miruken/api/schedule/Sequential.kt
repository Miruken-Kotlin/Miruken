package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.callback.Handling
import com.miruken.api.send

class Sequential (
    override val requests: List<NamedType>,
    override val typeName: String =
            "Miruken.Mediate.Schedule.Sequential,Miruken.Mediate"
) : Scheduled

fun Handling.sequential(requests: List<NamedType>) =
        send(Sequential(requests))

fun Handling.sequential(vararg requests: NamedType) =
        send(Sequential(requests.asList()))