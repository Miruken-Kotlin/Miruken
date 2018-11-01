package com.miruken.api.oneway

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf
import kotlin.reflect.KType

data class Oneway<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType,
        override val typeName: String =
                "Miruken.Mediate.Oneway.Oneway`1[[${request.typeName}]],Miruken.Mediate"
) : NamedType

inline val <TResp: Any, reified T: Request<TResp>>
        T.oneway: Oneway<TResp> get ()= Oneway(this, typeOf<T>())