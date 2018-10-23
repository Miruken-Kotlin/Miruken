package com.miruken.mediate.route

import com.miruken.mediate.Request
import com.miruken.typeOf
import kotlin.reflect.KType

data class Routed<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType,
        val route:       String,
        val tag:         String? = null
) : Request<TResp>

inline fun <TResp: Any, reified T: Request<TResp>>
        T.routeTo(route: String, tag: String? = null
): Routed<TResp> = Routed(this, typeOf<T>(), route, tag)