package com.miruken.api.cache

import com.miruken.TypeReference
import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class CachedHandler
    @Provides @Singleton
    constructor(): Handler() {

    private val _cache = ConcurrentHashMap<NamedType, CacheResponse>()

    private data class CacheResponse(
            var response: Promise<*>, val lastUpdate: Instant)

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
            else -> Instant.now() > cached.lastUpdate +
                    (request.timeToLive ?: ONE_DAY)
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
    ): CacheResponse {
        val response = composer.send(request, requestType)
        val cached   = CacheResponse(response, Instant.now())
        response then { cached.response = Promise.resolve(it) }
        return cached
    }
}

private val ONE_DAY = Duration.ofDays(1)