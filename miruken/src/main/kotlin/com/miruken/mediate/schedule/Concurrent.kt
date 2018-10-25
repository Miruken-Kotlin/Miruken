package com.miruken.mediate.schedule

import com.miruken.callback.Handling
import com.miruken.mediate.send

data class Concurrent(
        override val requests: List<Any>
) : Scheduled

fun Handling.concurrent(requests: List<Any>) =
        send(Concurrent(requests))

fun Handling.concurrent(vararg requests: Any) =
        send(Concurrent(requests.asList()))