package com.miruken.api.schedule

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.Try

interface Scheduled : Request<ScheduleResult> {
    val requests: List<NamedType>
}

data class ScheduleResult(
        val responses: List<Try<Throwable, NamedType>>
)
