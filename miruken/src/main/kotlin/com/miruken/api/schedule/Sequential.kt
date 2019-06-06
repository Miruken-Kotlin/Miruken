package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.Handling

class Sequential (
    override val requests: List<NamedType>
) : Scheduled {
    override val typeName: String = Sequential.typeName

    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Schedule.Sequential,Miruken"
    }
}

fun Handling.sequential(requests: List<NamedType>) =
        send(Sequential(requests))

fun Handling.sequential(vararg requests: NamedType) =
        send(Sequential(requests.asList()))