package com.miruken.concurrent

import com.miruken.Either
import com.miruken.event.Event
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class PromiseState {
    PENDING,
    FULFILLED,
    REJECTED,
    CANCELLED
}

enum class ChildCancelMode { ALL, ANY }

typealias PromiseExecutorBlock<T> =
        ((T) -> Unit, (Throwable) -> Unit) -> Unit

typealias PromiseExecutorCancelBlock<T> =
        ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit

typealias PromiseSuccessBlock<T, R> = (T) -> R
typealias PromiseFailureBlock<S>    = (Throwable) -> S
typealias PromiseFinalBlock<R>      = () -> R
typealias PromiseCancelledBlock     = (CancellationException) -> Unit

open class Promise<out T>
    private constructor(val cancelMode: ChildCancelMode) {

    private var _fulfilled  = Event<T>()
    private var _rejected   = Event<Throwable>()
    private var _onCancel   : (() -> Unit) = {}
    private var _result     : Any? = null
    private var _throwable  : Throwable? = null
    private val _completed  : AtomicBoolean = AtomicBoolean()
    private val _childCount : AtomicInteger = AtomicInteger()
    private val _guard      = java.lang.Object()

    @Volatile var state : PromiseState = PromiseState.PENDING
        protected set

    private val isCompleted get() = _completed.get()

    constructor(
            executor: PromiseExecutorBlock<T>
    ) : this(ChildCancelMode.ALL, executor)

    constructor(
            mode:     ChildCancelMode,
            executor: PromiseExecutorBlock<T>
    ) : this(mode) {
        try {
            executor(::resolve, ::reject)
        } catch (e: Throwable) {
            reject(e)
        }
    }

    constructor(
            executor: PromiseExecutorCancelBlock<T>
    ) : this(ChildCancelMode.ALL, executor)

    constructor(
            mode:     ChildCancelMode,
            executor: PromiseExecutorCancelBlock<T>
    ) : this(mode) {
        try {
            executor(::resolve, ::reject) { _onCancel = it }
        } catch (e: Throwable) {
            reject(e)
        }
    }

    infix fun <R> then(success: PromiseSuccessBlock<T, R>): Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((T) -> Unit) = {
                try {
                    resolveChild(success(it))
                } catch (e: Throwable) {
                    rejectChild(e)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    @Suppress("UNCHECKED_CAST")
                    if (state == PromiseState.FULFILLED)
                        res(_result as T)
                    else
                        rejectChild(_throwable!!)
                }
                else {
                    _fulfilled += res
                    _rejected  += rejectChild
                }
            }
        }
    }

    fun <R, S> then(
            success: PromiseSuccessBlock<T, R>,
            fail:    PromiseFailureBlock<S>
    ): Promise<Either<S, R>> {
        return createChild { resolveChild, rejectChild ->
            val res: ((T) -> Unit) = {
                try {
                    resolveChild(Either.Right(success(it)))
                } catch (e: Throwable) {
                    rejectChild(e)
                }
            }
            val rej: ((Throwable) -> Unit) = {
                if (_throwable !is CancellationException) {
                    try {
                        resolveChild(Either.Left(fail(_throwable!!)))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(_throwable!!)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    @Suppress("UNCHECKED_CAST")
                    if (state == PromiseState.FULFILLED)
                        res(_result as T)
                    else
                        rej(_throwable!!)
                }
                else {
                    _fulfilled += res
                    _rejected  += rej
                }
            }
        }
    }

    infix fun <R> catch(
            fail: PromiseFailureBlock<R>
    ): Promise<Either<R, T>> {
        return createChild { resolveChild, rejectChild ->
            val res: ((T) -> Unit) =  {
                resolveChild(Either.Right(it))
            }
            val rej: ((Throwable) -> Unit) = {
                if (_throwable !is CancellationException) {
                    try {
                        resolveChild(Either.Left(fail(_throwable!!)))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(_throwable!!)
                }
            }
            synchronized (_guard) {
                if (isCompleted)
                {
                    @Suppress("UNCHECKED_CAST")
                    if (state == PromiseState.FULFILLED)
                        res(_result as T)
                    else
                        rej(_throwable!!)
                }
                else
                {
                    _fulfilled += res
                    _rejected  += rej
                }
            }
        }
    }

    infix fun <R> finally(final: PromiseFinalBlock<R>): Promise<T> {
        return createChild { resolveChild, rejectChild ->
            val res: ((T) -> Unit) = {
                try {
                    val result = final()
                    @Suppress("UNCHECKED_CAST")
                    if (result is Promise<*>) {
                        result.then(
                            { resolveChild(_result as T) },
                            rejectChild)
                    } else {
                        resolveChild(_result as T)
                    }
                } catch (e: Throwable) {
                    rejectChild(e)
                }
            }
            val rej: ((Throwable) -> Unit) = {
                try {
                    final()
                    rejectChild(it)
                } catch (t: Throwable) {
                    rejectChild(t)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    @Suppress("UNCHECKED_CAST")
                    if (state == PromiseState.FULFILLED)
                        res(_result as T)
                    else
                        rej(_throwable!!)
                }
                else {
                    _fulfilled += res
                    _rejected  += rej
                }
            }
        }
    }

    fun cancel() {
        reject(CancellationException())
    }

    infix fun cancelled(cancelled: PromiseCancelledBlock): Promise<T> {
        synchronized (_guard) {
            if (isCompleted) {
                if (state == PromiseState.CANCELLED) {
                    cancelled(_throwable as CancellationException)
                }
            } else {
                _rejected += { e ->
                    if (e is CancellationException)
                        cancelled(e)
                }
            }
        }
        return this
    }

    fun get(timeoutMs: Long? = null): T {
        val deadline = timeoutMs?.let {
            Instant.now().plusMillis(timeoutMs) }
        synchronized (_guard) {
            while (!isCompleted &&
                    deadline?.let { Instant.now() < deadline } != false) {
                if (deadline == null)
                    _guard.wait()
                else
                    _guard.wait(Duration.between(Instant.now(),
                            deadline).toMillis())
                if (!isCompleted)
                    throw TimeoutException()
            }
        }
        if (_throwable != null) throw _throwable!!
        @Suppress("UNCHECKED_CAST")
        return _result as T
    }

    protected open fun <R> createChild(
            executor: PromiseExecutorBlock<R>
    ): Promise<R> {
        val child = createChild<R>(cancelMode) { resolve, reject, onCancel ->
            executor(resolve, reject)
            onCancel {
                if (cancelMode == ChildCancelMode.ANY ||
                        _childCount.decrementAndGet() == 0)
                    cancel()
            }
        }
        if (cancelMode == ChildCancelMode.ALL)
            _childCount.incrementAndGet()
        return child
    }

    protected open fun <R> createChild(
        mode:     ChildCancelMode,
        executor: PromiseExecutorCancelBlock<R>
    ): Promise<R> = Promise(mode, executor)

    private fun resolve(result: T) {
        if (_completed.compareAndSet(false, true)) {
            _result = result
            synchronized (_guard) {
                state = PromiseState.FULFILLED
                _rejected.clear()
                _guard.notifyAll()
                _fulfilled(result)
                _fulfilled.clear()
            }
        }
    }

    private fun reject(e: Throwable) {
        if (_completed.compareAndSet(false, true)) {
            _throwable = e
            val isCancellation = e is CancellationException
            if (isCancellation) {
                try {
                    _onCancel()
                }
                catch (t: Throwable) {
                    e.addSuppressed(t)
                }
            }
            synchronized (_guard) {
                state = if (isCancellation) PromiseState.CANCELLED
                        else PromiseState.REJECTED
                _fulfilled.clear()
                _guard.notifyAll()
                _rejected(e)
                _rejected.clear()
            }
        }
    }

    companion object {
        val TRUE       = resolve(true)
        val FALSE      = resolve(false)
        val EMPTY      = resolve(null as Any?)
        val EMPTY_LIST = resolve(emptyList<Any>())

        fun reject(e: Throwable): Promise<Nothing> =
                Promise { _, reject -> reject(e) }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified S: Any?> resolve(result: S): Promise<S> =
                if (S::class === Any::class && result is Promise<*>) {
                    Promise<Any?> { suc, fail ->
                        result.then({ suc(it)}, fail) } as Promise<S>
                } else {
                    Promise { success, _ -> success(result) }
                }

        fun <S> resolve(promise: Promise<S>): Promise<S> = promise
    }
}

fun <T> Promise<Promise<T>>.flatten(): Promise<T> {
    return Promise(this.cancelMode) { resolve, reject, onCancel ->
        onCancel { this.cancel() }
        then({ inner -> inner.then(resolve, reject)
                .cancelled(reject)}, reject)
                .cancelled(reject)
    }
}
