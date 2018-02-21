package com.miruken.callback

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