package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.protocol.proxy

class Batch(vararg tags: Any) :
        CompositeHandler(), BatchingComplete {

    private val _tags = tags.toSet()

    fun shouldBatch(tag: Any): Boolean {
        return _tags.isEmpty() || _tags.contains(tag)
    }

    override fun completeBatch(composer: Handling): Promise<List<Any?>> =
        Promise.all(handlers.map {
            it.proxy<Batching>().complete(composer) })
}