package com.miruken.api.cache

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.RequestWrapper
import com.miruken.api.responseTypeName
import com.miruken.typeOf
import java.time.Duration

data class Cached<TResp: NamedType>(
        override val request:     Request<TResp>,
        override val requestType: TypeReference,
        val timeToLive: Duration? = null
) : RequestWrapper<TResp> {
    override val typeName: String =
            Cached.typeName.format(request.responseTypeName)

    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Cache.Cached`1[[%s]],Miruken.Mediate"
    }
}

data class Refresh<TResp: NamedType>(
        override val request:     Request<TResp>,
        override val requestType: TypeReference,
        val timeToLive:  Duration? = null
) : RequestWrapper<TResp> {
    override val typeName: String =
            Refresh.typeName.format(request.responseTypeName)

    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Cache.Refresh`1[[%s]],Miruken.Mediate"
    }
}

data class Invalidate<TResp: NamedType>(
        override val request:     Request<TResp>,
        override val requestType: TypeReference
) : RequestWrapper<TResp?> {
    override val typeName: String =
            Invalidate.typeName.format(request.responseTypeName)

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
