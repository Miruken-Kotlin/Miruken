package com.miruken.mediate.cache

import com.miruken.mediate.Request
import com.miruken.typeOf
import java.time.Duration
import kotlin.reflect.KType

data class Cached<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType,
        val timeToLive:  Duration? = null
) : Request<TResp>

data class Refresh<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType,
        val timeToLive:  Duration? = null
) : Request<TResp>

data class Invalidate<TResp: Any>(
        val request:     Request<TResp>,
        val requestType: KType
) : Request<TResp?>

inline fun <TResp: Any, reified T: Request<TResp>>
        T.cache(timeToLive: Duration? = null
): Cached<TResp> = Cached(this, typeOf<T>(), timeToLive)

inline fun <TResp: Any, reified T: Request<TResp>>
        T.refresh(timeToLive: Duration? = null
): Refresh<TResp> =
        Refresh(this, typeOf<T>(), timeToLive)

inline fun <TResp: Any, reified T: Request<TResp>>
        T.invalidate(): Invalidate<TResp> =
        Invalidate(this, typeOf<T>())
