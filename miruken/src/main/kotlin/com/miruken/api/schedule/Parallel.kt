package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.callback.Handling
import com.miruken.api.send

class Parallel(
    override val requests: List<NamedType>,
    override val typeName: String = Parallel.typeName
) : Scheduled {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Schedule.Parallel,Miruken.Mediate"
    }
}

fun Handling.parallel(requests: List<NamedType>) =
        send(Parallel(requests))

fun Handling.parallel(vararg requests: NamedType) =
        send(Parallel(requests.asList()))