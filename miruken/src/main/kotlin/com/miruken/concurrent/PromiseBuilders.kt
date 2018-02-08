package com.miruken.concurrent

import java.util.concurrent.atomic.AtomicInteger

/**
fun Promise.Companion.all(results: Collection<Any>) : Promise<Array<Any>> {
    if (results.isEmpty())
        return Promise.resolve(emptyArray())

    var pending   = AtomicInteger(0)
    val promises  = results.map(::resolve)
    var fulfilled = arrayOfNulls<Any>(promises.size)
}
*/