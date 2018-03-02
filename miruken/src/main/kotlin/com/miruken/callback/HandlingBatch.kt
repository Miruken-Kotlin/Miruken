package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf

fun Handling.batch(vararg tags: Any) =
        BatchingHandler(this, tags)

fun Handling.noBatch() = NoBatchingHandler(this)

fun Handling.batch(
        prepare: (BatchingHandler) -> Unit
): Promise<List<Any?>> {
    val batch = BatchingHandler(this)
    prepare(batch)
    return batch.complete()
}

fun Handling.batch(
        vararg tags: Any,
        prepare:     (BatchingHandler) -> Unit
): Promise<List<Any?>> {
    val batch = BatchingHandler(this, *tags)
    prepare(batch)
    return batch.complete()
}

inline fun <reified T> Handling.batchOver(
        prepare: (BatchingHandler) -> Unit
): Promise<List<Any?>> {
    val batch = BatchingHandler(this, typeOf<T>())
    prepare(batch)
    return batch.complete()
}

fun Handling.getBatch(tag: Any? = null): Batch? =
        resolve<Batch>()?.takeIf {
            tag == null || it.shouldBatch(tag)
        }

inline fun <reified B: Batching> Handling.getBatcherFor(
        tag:     Any? = null,
        factory: () -> B
): B? = getBatch(tag)?.let { batch ->
    factory().apply { batch.addHandlers(this) }
}

inline fun <reified T: Any, reified B: Batching> Handling.getBatcherFor(
        factory: () -> B
): B? = getBatch(typeOf<T>())?.let { batch ->
    factory().apply { batch.addHandlers(this) }
}