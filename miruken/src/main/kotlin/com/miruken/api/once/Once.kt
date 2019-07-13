package com.miruken.api.once

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.typeOf
import java.util.*

data class Once(
        val request:     NamedType,
        val requestType: TypeReference? = null,
        val requestId:   UUID = UUID.randomUUID()
) : NamedType {
    override val typeName: String = Once.typeName

    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Once.Once,Miruken"
    }
}

inline val <reified T: NamedType>
        T.once: Once get () = Once(this, typeOf<T>())

inline fun <reified T: NamedType>
        T.once(requestId: UUID) = Once(this, typeOf<T>(), requestId)