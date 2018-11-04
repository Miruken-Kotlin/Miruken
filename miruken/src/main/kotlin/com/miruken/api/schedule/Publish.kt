package com.miruken.api.schedule

import com.miruken.api.NamedType

data class Publish(
        val          message:  NamedType,
        override val typeName: String = Publish.typeName
) : NamedType {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Schedule.Publish,Miruken.Mediate"
    }
}