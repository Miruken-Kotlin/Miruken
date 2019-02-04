package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.concurrent.Promise
import com.miruken.concurrent.flatten
import com.miruken.protocol.proxy
import com.miruken.runtime.isGeneric
import java.util.concurrent.atomic.AtomicInteger

class BatchingHandler(
        handler: Handling,
        vararg tags: Any
) : DecoratedHandler(handler) {

    private val _completed = AtomicInteger(0)

    @get:Provides
    var batch: Batch? = Batch(*tags)
        private set

    @Provides
    @Suppress("UNCHECKED_CAST")
    fun <T: Batching> getBatch(inquiry: Inquiry): T? {
        return batch?.let {
            (it.findHandler(inquiry.key) as? T) ?:
            (inquiry.createKeyInstance() as? T)?.apply {
                if (inquiry.key is TypeReference && this::class.isGeneric) {
                    it.addHandlers(GenericWrapper(this, inquiry.key))
                } else {
                    it.addHandlers(this)
                }
            }
        }
    }

    override fun handleCallback(
            callback:     Any,
            callbackType: TypeReference?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        return batch?.takeIf {
                (callback as? BatchingCallback)?.canBatch != false
            }?.let { b ->
                if (_completed.get() > 0 && callback !is Composition) {
                    batch = null
                }
                b.handle(callback, callbackType, greedy, composer)
                        .takeIf { it.stop || it.handled }
            } ?: super.handleCallback(
                callback, callbackType, greedy, composer)
    }

    fun complete(promise: Promise<*>? = null): Promise<List<Any?>> {
        if (!_completed.compareAndSet(0, 1))
            error("The batch already completed")
        val results = proxy<BatchingComplete>().completeBatch(this)
        return promise?.let { follow ->
            results then { rs -> follow.then { rs } }
        }?.flatten() ?: results
    }
}