package com.miruken.api.schedule

import com.miruken.api.NamedType

data class Publish(val message: Any) : NamedType {
    override val typeName: String
        get() = "Miruken.Mediate.Schedule.Publish,Miruken.Mediate"
}