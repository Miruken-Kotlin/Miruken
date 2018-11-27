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
    @Suppress("UNCHECKED_CAST")
    fun <TResp: NamedType> cache(
        request:  Cached<TResp>,
        composer: Handling
    ): Promise<TResp> {
        val key     = request.request
        var created = false
        var cached  = _cache.getOrPut(key) {
            created = true
            refreshResponse(key, request.requestType, composer)
        }
        if (!created && when(cached.response.state) {
            PromiseState.REJECTED -> true
            PromiseState.CANCELLED -> true
            else -> {
                val expiration = request.timeToLive ?: ONE_DAY
                Instant.now() > cached.lastUpdate + expiration
            }
        }) {
            cached = refreshResponse(key, request.requestType, composer)
            _cache[key] = cached
        }
        return cached.response as Promise<TResp>
    }

    @Handles
    @Suppress("UNCHECKED_CAST")
    fun <TResp: NamedType> refresh(
            request:  Refresh<TResp>,
            composer: Handling
    ): Promise<TResp> {
        val key = request.request
        return refreshResponse(key, request.requestType, composer).also {
            _cache[key] = it
        }.response as Promise<TResp>
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
    ) = CacheResponse(composer.send(request, requestType), Instant.now())
}

private val ONE_DAY = Duration.ofDays(1)