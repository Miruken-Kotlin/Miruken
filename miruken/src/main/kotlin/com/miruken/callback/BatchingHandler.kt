package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.unwrap
import com.miruken.protocol.proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KType

class BatchingHandler(
        handler: Handling,
        vararg tags: Any
) : DecoratedHandler(handler) {

    private val _completed = AtomicInteger(0)

    @Provides
    var batch: Batch? = Batch(*tags)
        private set

    @Provides
    @Suppress("UNCHECKED_CAST")
    fun <T: Batching> getBatch(inquiry: Inquiry): T? {
        return batch?.let {
            inquiry.createKeyInstance() as? T
        }?.apply { batch!!.addHandlers(this) }
    }

    override fun handleCallback(
            callback:     Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        return batch?.let {
            it.takeIf { (callback as? BatchingCallback)
                ?.canBatch != false }
                ?.let {
                    if (_completed.get() > 0 && callback !is Composition)
                        batch = null
                    it.handle(callback, callbackType, greedy, composer)
                            .takeIf { it.stop || it.handled }
                }
        } ?: super.handleCallback(
                callback, callbackType, greedy, composer)
    }

    fun complete(promise: Promise<*>? = null): Promise<List<Any?>> {
        if (!_completed.compareAndSet(0, 1))
            error("The batch already completed")
        val results = proxy<BatchingComplete>().completeBatch(this)
        return promise?.let { follow ->
            results then { rs -> follow.then { rs } }
        }?.unwrap() ?: results
    }
}