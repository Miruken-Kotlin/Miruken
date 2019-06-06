package com.miruken.api.oneway

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.responseTypeName
import com.miruken.typeOf

data class Oneway<TResp: NamedType>(
        val request:     Request<TResp>,
        val requestType: TypeReference
) : NamedType {
    override val typeName: String =
            Oneway.typeName.format(request.responseTypeName)

    companion object : NamedType {
        override val typeName =
                "Miruken.Api.Oneway.Oneway`1[[%s]],Miruken"
    }
}

inline val <TResp: NamedType, reified T: Request<TResp>>
        T.oneway: Oneway<TResp> get () = Oneway(this, typeOf<T>())