package com.miruken.api.oneway

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf

data class Oneway(
        val request:     NamedType,
        val requestType: TypeReference
) : NamedType {
    override val typeName: String = Oneway.typeName

    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Oneway.Oneway,Miruken"
    }
}

inline val <TResp: NamedType, reified T: Request<TResp>>
        T.oneway: Oneway get () = Oneway(this, typeOf<T>())