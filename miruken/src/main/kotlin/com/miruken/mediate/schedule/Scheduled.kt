package com.miruken.mediate.schedule

import com.miruken.Either
import com.miruken.mediate.Request

interface Scheduled : Request<ScheduleResult> {
    val requests: List<Any>
}

data class ScheduleResult(
        val responses: List<Either<Exception, Any>>
)