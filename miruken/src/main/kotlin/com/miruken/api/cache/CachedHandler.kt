package com.miruken.api.cache

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.Handler
import com.miruken.callback.Handles
import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import java.util.concurrent.ConcurrentHashMap

class CachedHandler : Handler() {
    private val _cache = ConcurrentHashMap<NamedType, CacheResponse>()

    private data class CacheResponse(
            val response:   Promise<*>,
            val lastUpdate: Instant)

    @Handles
    fun <TResp: NamedType> cache(
        request:  Cached<TResp>,
        composer: Handling
    ): Promise<TResp> {
        @Suppress("UNCHECKED_CAST")
        return _cache.compute(request.request) { key, cached ->
            cached?.takeUnless {
                when(it.response.state) {
                    PromiseState.REJECTED -> true
                    PromiseState.CANCELLED -> true
                    else -> {
                        val expiration = request.timeToLive ?: ONE_DAY
                        Instant.now() > it.lastUpdate + expiration
                    }
                }
            } ?: refreshResponse(key, request.requestType, composer)
        }!!.response as Promise<TResp>
    }

    @Handles
    fun <TResp: NamedType> refresh(
            request:  Refresh<TResp>,
            composer: Handling
    ): Promise<TResp> {
        @Suppress("UNCHECKED_CAST")
        return _cache.compute(request.request) { key, _ ->
            refreshResponse(key, request.requestType, composer)
        }!!.response as Promise<TResp>
    }

    @Handles
    @Suppress("UNCHECKED_CAST")
    fun <TResp: NamedType> invalidate(
            request:  Invalidate<TResp>
    ) = (_cache.remove(request.request)?.response
                ?: Promise.EMPTY) as Promise<TResp>

    private fun refreshResponse(
            request:     NamedType,
            requestType: TypeReference,
            composer:    Handling
    ) = CacheResponse(
        composer.send(request, requestType),
        Instant.now()
    )
}

private val ONE_DAY = Duration.ofDays(1)