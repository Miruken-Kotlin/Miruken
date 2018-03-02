package com.miruken.callback

import com.miruken.concurrent.Promise

interface Batching {
    fun complete(composer: Handling): Any?
}

interface BatchingComplete {
    fun completeBatch(composer: Handling): Promise<List<Any?>>
}