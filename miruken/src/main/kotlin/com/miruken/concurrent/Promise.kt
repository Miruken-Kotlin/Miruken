package com.miruken.concurrent

import com.miruken.Either
import com.miruken.event.Event
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
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
    private var _onCancel   = Event<Unit>()
    private var _result     : Any? = null
    private var _throwable  : Throwable? = null
    private var _completed  : Boolean = false
    private val _childCount : AtomicInteger = AtomicInteger()
    private val _guard      = Object()

    @Volatile var state : PromiseState = PromiseState.PENDING
        protected set

    private val isCompleted get() = _completed

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
            executor(::resolve, ::reject) { onCancel ->
                _onCancel += { onCancel() }
            }
        } catch (e: Throwable) {
            reject(e)
        }
    }

    constructor(resolved: T) : this(ChildCancelMode.ANY) {
        _result    = resolved
        state      = PromiseState.FULFILLED
        _completed = true
    }

    constructor(rejected: Throwable) : this(ChildCancelMode.ANY) {
        _throwable = rejected
        state      = when (rejected){
            is CancellationException -> PromiseState.CANCELLED
            else -> PromiseState.REJECTED
        }
        _completed = true
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
            subscribe(res, rejectChild)
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
            subscribe(res, rej)
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
            subscribe(res, rej)
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
            subscribe(res, rej)
        }
    }

    fun cancel() {
        reject(CancellationException())
    }

    infix fun cancelled(cancelled: PromiseCancelledBlock): Promise<T> {
        synchronized (_guard) {
            if (!isCompleted) {
                _rejected += { e ->
                    if (e is CancellationException)
                        cancelled(e)
                }
                return this
            }
        }
        if (state == PromiseState.CANCELLED) {
            cancelled(_throwable as CancellationException)
        }
        return this
    }

    fun get(timeoutMs: Long? = null): T {
        if (!isCompleted) {
            val deadline = timeoutMs?.let {
                Instant.now().plusMillis(timeoutMs)
            }
            synchronized(_guard) {
                while (!isCompleted && deadline?.let { Instant.now() < deadline } != false) {
                    if (deadline == null) {
                        _guard.wait()
                    } else {
                        _guard.wait(Duration.between(Instant.now(), deadline).toMillis())
                    }
                    if (!isCompleted) throw TimeoutException()
                }
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
        if (_completed) return

        var fulfilled: Event<T>?

        synchronized(_guard) {
            if (_completed) return
            _completed = true
            _result    = result
            state      = PromiseState.FULFILLED
            fulfilled  = _fulfilled
            _fulfilled = Event()
            _rejected  = Event()
            _onCancel  = Event()
            _guard.notifyAll()
        }

        fulfilled?.invoke(result)
    }

    private fun reject(e: Throwable) {
        if (_completed) return

        var onCancel: Event<Unit>? = null
        var rejected: Event<Throwable>?

        synchronized(_guard) {
            if (_completed) return
            _completed = true
            _throwable = e
            val isCancellation = e is CancellationException
            if (isCancellation) {
                onCancel = _onCancel
            }
            state = if (isCancellation) PromiseState.CANCELLED
                    else PromiseState.REJECTED
            rejected   = _rejected
            _fulfilled = Event()
            _rejected  = Event()
            _onCancel  = Event()
            _guard.notifyAll()
        }

        if (onCancel != null) {
            try {
                onCancel!!(Unit)
            }
            catch (t: Throwable) {
                e.addSuppressed(t)
            }
        }

        rejected?.invoke(e)
    }

    @Suppress("UNCHECKED_CAST")
    private fun subscribe(resolve: ((T) -> Unit), reject: ((Throwable) -> Unit)) {
        synchronized (_guard) {
            if (!isCompleted) {
                _fulfilled += resolve
                _rejected  += reject
                return
            }
        }
        if (state == PromiseState.FULFILLED)
            resolve(_result as T)
        else
            reject(_throwable!!)
    }

    companion object {
        val TRUE       = Promise(true)
        val FALSE      = Promise(false)
        val EMPTY      = Promise(null as Any?)
        val EMPTY_LIST = Promise(emptyList<Any>())

        fun reject(error: Throwable): Promise<Nothing> = Promise(error)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified S: Any?> resolve(result: S): Promise<S> =
                if (S::class === Any::class && result is Promise<*>) {
                    Promise<Any?>(result.cancelMode) { suc, fail, onCancel ->
                        onCancel(result::cancel)
                        result.then(suc, fail).cancelled(fail)
                    } as Promise<S>
                } else {
                    Promise(result)
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
