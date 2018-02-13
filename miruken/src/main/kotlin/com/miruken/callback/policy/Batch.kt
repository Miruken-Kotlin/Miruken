package com.miruken.callback.policy

import com.miruken.callback.BatchingComplete
import com.miruken.callback.CompositeHandler
import com.miruken.callback.Handling
import com.miruken.concurrent.Promise

class Batch(vararg tags: Any) :
        CompositeHandler(), BatchingComplete {

    private val _tags = mutableSetOf(tags)

    fun shouldBatch(tag: Any): Boolean {
        return _tags.isEmpty() || _tags.contains(tag)
    }

    override fun complete(composer: Handling): Promise<List<Any>> {
        TODO("not implemented")
    }
}