package com.miruken.mediate.oneway

import com.miruken.mediate.Request
import com.miruken.typeOf
import kotlin.reflect.KType

data class Oneway<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType
)

inline fun <TResp: Any, reified T: Request<TResp>>
        T.oneway(): Oneway<TResp> = Oneway(this, typeOf<T>())