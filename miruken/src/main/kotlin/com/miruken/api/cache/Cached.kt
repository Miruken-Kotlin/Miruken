package com.miruken.api.cache

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf
import java.time.Duration
import kotlin.reflect.KType

data class Cached<TResp: NamedType>(
        val request:     Request<TResp>,
        val requestType: KType,
        val timeToLive:  Duration? = null,
        override val typeName: String =
                Cached.typeName.format(request.typeName)
) : Request<TResp> {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Cache.Cached`1[[%s]],Miruken.Mediate"
    }
}

data class Refresh<TResp: NamedType>(
        val request:     Request<TResp>,
        val requestType: KType,
        val timeToLive:  Duration? = null,
        override val typeName: String =
                Refresh.typeName.format(request.typeName)
) : Request<TResp> {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Cache.Refresh`1[[%s]],Miruken.Mediate"
    }
}

data class Invalidate<TResp: NamedType>(
        val request:     Request<TResp>,
        val requestType: KType,
        override val typeName: String =
                Invalidate.typeName.format(request.typeName)
) : Request<TResp?> {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Cache.Invalidate`1[[%s]],Miruken.Mediate"
    }
}

inline fun <TResp: NamedType, reified T: Request<TResp>>
        T.cache(timeToLive: Duration? = null
): Cached<TResp> = Cached(this, typeOf<T>(), timeToLive)

inline fun <TResp: NamedType, reified T: Request<TResp>>
        T.refresh(timeToLive: Duration? = null
): Refresh<TResp> =
        Refresh(this, typeOf<T>(), timeToLive)

inline fun <TResp: NamedType, reified T: Request<TResp>>
        T.invalidate(): Invalidate<TResp> =
        Invalidate(this, typeOf<T>())
