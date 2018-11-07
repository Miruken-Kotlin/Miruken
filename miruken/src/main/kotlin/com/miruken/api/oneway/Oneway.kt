package com.miruken.api.oneway

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf

data class Oneway<TResp: NamedType>(
        val request:     Request<TResp>,
        val requestType: TypeReference,
        override val typeName: String =
                Oneway.typeName.format(request.typeName)
) : NamedType {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Oneway.Oneway`1[[%s]],Miruken.Mediate"
    }
}

inline val <TResp: NamedType, reified T: Request<TResp>>
        T.oneway: Oneway<TResp> get ()= Oneway(this, typeOf<T>())