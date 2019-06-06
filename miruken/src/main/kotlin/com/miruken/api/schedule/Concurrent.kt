package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.Handling

data class Concurrent(
        override val requests: List<NamedType>,
        override val typeName: String = Concurrent.typeName
) : Scheduled {
    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Schedule.Concurrent,Miruken"
    }
}

fun Handling.concurrent(requests: List<NamedType>) =
        send(Concurrent(requests))

fun Handling.concurrent(vararg requests: NamedType) =
        send(Concurrent(requests.asList()))