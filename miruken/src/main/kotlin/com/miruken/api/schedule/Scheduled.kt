package com.miruken.api.schedule

import com.miruken.Either
import com.miruken.api.Request

interface Scheduled : Request<ScheduleResult> {
    val requests: List<Any>
}

data class ScheduleResult(
        val responses: List<Either<Exception, Any>>
)