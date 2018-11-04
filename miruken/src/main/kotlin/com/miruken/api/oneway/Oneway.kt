package com.miruken.api.oneway

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf
import kotlin.reflect.KType

data class Oneway<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType,
        override val typeName: String =
                Oneway.typeName.format(request.typeName)
) : NamedType {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Oneway.Oneway`1[[%s]],Miruken.Mediate"
    }
}

inline val <TResp: Any, reified T: Request<TResp>>
        T.oneway: Oneway<TResp> get ()= Oneway(this, typeOf<T>())