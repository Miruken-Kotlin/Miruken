package com.miruken.mediate.cache

import com.miruken.callback.Handler
import com.miruken.callback.Handles
import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.mediate.send
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

class CachedHandler : Handler() {
    private val _cache = ConcurrentHashMap<Any, CacheResponse>()

    private data class CacheResponse(
            val response:   Promise<*>,
            val lastUpdate: Instant)

    @Handles
    fun <TResponse: Any> cache(
        request:  Cached<TResponse>,
        composer: Handling
    ): Promise<TResponse> {
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
        }!!.response as Promise<TResponse>
    }

    @Handles
    fun <TResponse: Any> refresh(
            request:  Refresh<TResponse>,
            composer: Handling
    ): Promise<TResponse> {
        @Suppress("UNCHECKED_CAST")
        return _cache.compute(request.request) { key, _ ->
            refreshResponse(key, request.requestType, composer)
        }!!.response as Promise<TResponse>
    }

    @Handles
    @Suppress("UNCHECKED_CAST")
    fun <TResponse: Any> invalidate(
            request:  Invalidate<TResponse>
    ) = (_cache.remove(request.request)?.response
                ?: Promise.EMPTY) as Promise<TResponse>

    private fun refreshResponse(
            request:     Any,
            requestType: KType,
            composer:    Handling
    ) = CacheResponse(
        composer.send(request, requestType),
        Instant.now()
    )
}

private val ONE_DAY = Duration.ofDays(1)