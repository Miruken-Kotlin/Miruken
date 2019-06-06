package com.miruken.api.schedule

import com.miruken.api.NamedType

data class Publish(
        val message: NamedType
) : NamedType {
    override val typeName: String = Publish.typeName

    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Schedule.Publish,Miruken"
    }
}