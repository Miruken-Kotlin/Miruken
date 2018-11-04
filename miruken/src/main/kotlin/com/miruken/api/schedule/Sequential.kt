package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.callback.Handling
import com.miruken.api.send

class Sequential (
    override val requests: List<NamedType>,
    override val typeName: String = Sequential.typeName
) : Scheduled {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Schedule.Sequential,Miruken.Mediate"
    }
}

fun Handling.sequential(requests: List<NamedType>) =
        send(Sequential(requests))

fun Handling.sequential(vararg requests: NamedType) =
        send(Sequential(requests.asList()))