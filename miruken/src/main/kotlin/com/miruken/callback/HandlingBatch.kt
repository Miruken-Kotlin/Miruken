package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.await
import com.miruken.typeOf

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

suspend fun Handling.batchCo(
        prepare: (BatchingHandler) -> Unit
):List<Any?> {
    val batch = BatchingHandler(this)
    prepare(batch)
    return batch.complete().await()
}

fun Handling.batch(
        vararg tags: Any,
        prepare:     (BatchingHandler) -> Unit
): Promise<List<Any?>> {
    val batch = BatchingHandler(this, *tags)
    prepare(batch)
    return batch.complete()
}

suspend fun Handling.batchCo(
        vararg tags: Any,
        prepare:     (BatchingHandler) -> Unit
): List<Any?> {
    val batch = BatchingHandler(this, *tags)
    prepare(batch)
    return batch.complete().await()
}

inline fun <reified T> Handling.batchOver(
        prepare: (BatchingHandler) -> Unit
): Promise<List<Any?>> {
    val batch = BatchingHandler(this, typeOf<T>())
    prepare(batch)
    return batch.complete()
}

suspend inline fun <reified T> Handling.batchOverCo(
        prepare: (BatchingHandler) -> Unit
): List<Any?> {
    val batch = BatchingHandler(this, typeOf<T>())
    prepare(batch)
    return batch.complete().await()
}

fun Handling.getBatch(tag: Any? = null): Batch? =
        resolve<Batch>()?.takeIf {
            tag == null || it.shouldBatch(tag)
        }

inline fun <reified B: Batching> Handling.getBatcher(
        tag:     Any? = null,
        factory: () -> B
): B? = getBatch(tag)?.let { batch ->
    (batch.findHandler(typeOf<B>()) as? B) ?:
    factory().apply { batch.addHandlers(this.toHandler()) }
}

inline fun <reified T: Any, reified B: Batching> Handling.getBatcher(
        factory: () -> B
): B? = getBatch(typeOf<T>())?.let { batch ->
    (batch.findHandler(typeOf<B>()) as? B) ?:
    factory().apply { batch.addHandlers(this.toHandler()) }
}