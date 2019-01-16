package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.Try

interface Scheduled : Request<ScheduledResult> {
    val requests: List<NamedType>
}

data class ScheduledResult(
        val responses: List<Try<Throwable, NamedType>>,
        override val typeName: String = ScheduledResult.typeName
) : NamedType {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Schedule.ScheduledResult,Miruken.Mediate"
    }
}
