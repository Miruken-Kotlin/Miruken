package com.miruken.concurrent

import com.miruken.*
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

open class Promise<out T>
    private constructor(val cancelMode: ChildCancelMode) {
    private var _fulfilled  : ((Any) -> Unit) = {}
    private var _rejected   : ((Throwable) -> Unit) = {}
    private var _onCancel   : (() -> Unit) = {}
    private var _result     : T? = null
    private var _throwable  : Throwable? = null
    private val _completed  : AtomicBoolean = AtomicBoolean()
    private val _childCount : AtomicInteger = AtomicInteger()
    private val _guard      = java.lang.Object()

    @Volatile var state : PromiseState = PromiseState.PENDING
        protected set

    private val isCompleted get() = _completed.get()

    constructor(
            executor: ((T) -> Unit, (Throwable) -> Unit) -> Unit
    ) : this(ChildCancelMode.ALL, executor)

    constructor(
            mode: ChildCancelMode,
            executor: ((T) -> Unit, (Throwable) -> Unit) -> Unit
    ) : this(mode) {
        try {
            executor(::resolve, ::reject)
        } catch (e: Throwable) {
            reject(e)
        }
    }

    constructor(
            executor: ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this(ChildCancelMode.ALL, executor)

    constructor(
            mode: ChildCancelMode,
            executor: ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this(mode) {
        try {
            executor(::resolve, ::reject) { _onCancel = it }
        } catch (e: Throwable) {
            reject(e)
        }
    }

    infix fun <R> then(success: ((T) -> R)) : Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = {
                try {
                    resolveChild(success(_result!!))
                } catch (e: Throwable) {
                    rejectChild(e)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    if (state == PromiseState.FULFILLED)
                        res(_result!!)
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

    fun <R, S> then(success: ((T) -> R), fail: ((Throwable) -> S)) : Promise<Either<S, R>> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = {
                try {
                    resolveChild(Either.Right(success(_result!!)))
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
                    if (state == PromiseState.FULFILLED)
                        res(_result!!)
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

    infix fun <R> catch(fail: ((Throwable) -> R)) : Promise<Either<R, T>> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) =  {
                resolveChild(Either.Right(_result!!))
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
                    if (state == PromiseState.FULFILLED)
                        res(_result!!)
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

    infix fun <R> finally(final: (() -> R)) : Promise<T> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = {
                try {
                    val result = final()
                    if (result is Promise<*>) {
                        result.then(
                            { _ -> resolveChild(_result!!) },
                            rejectChild)
                    } else {
                        resolveChild(_result!!)
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
                    if (state == PromiseState.FULFILLED)
                        res(_result!!)
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

    infix fun cancelled(cancelled: ((CancellationException) -> Unit)) : Promise<T> {
        synchronized (_guard) {
            if (isCompleted) {
                if (state == PromiseState.CANCELLED) {
                    cancelled(_throwable as CancellationException)
                }
            } else {
                _rejected +=  {
                    if (it is CancellationException)
                        cancelled(it)
                }
            }
        }
        return this
    }

    fun get(timeoutMs: Long? = null) : T {
        val deadline = timeoutMs?.let {
            Instant.now().plusMillis(timeoutMs) }
        synchronized (_guard) {
            while (!isCompleted &&
                    deadline?. let { Instant.now() < deadline } != false) {
                if (deadline == null)
                    _guard.wait()
                else
                    _guard.wait(Duration.between(Instant.now(),
                            deadline).toMillis())
                if (!isCompleted)
                    throw TimeoutException()
            }
        }
        if (_throwable != null)
            throw _throwable!!
        return _result!!
    }

    protected open fun <R> createChild(
            executor: ((R) -> Unit, (Throwable) -> Unit) -> Unit
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
        mode:  ChildCancelMode,
        executor: ((R) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : Promise<R>
    {
        return Promise(mode, executor)
    }

    private fun resolve(result: T) {
        if (_completed.compareAndSet(false, true)) {
            _result = result
            synchronized (_guard) {
                state = PromiseState.FULFILLED
                val fulfilled = _fulfilled
                _fulfilled = {}
                _rejected  = {}
                _guard.notifyAll()
                fulfilled(result as Any)
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
                    // consume errors
                }
            }
            synchronized (_guard) {
                state = if (isCancellation) PromiseState.CANCELLED
                        else PromiseState.REJECTED
                val rejected = _rejected
                _fulfilled = {}
                _rejected  = {}
                _guard.notifyAll()
                rejected(e)
            }
        }
    }

    companion object {
        val True      = resolve(true)
        val False     = resolve(false)
        val Empty     = resolve(Unit)
        val EmptyList = resolve(emptyList<Any>())

        fun reject(e: Throwable): Promise<Nothing> =
                Promise { _, reject -> reject(e) }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified S: Any> resolve(result: S): Promise<S> =
                if (S::class === Any::class && result is Promise<*>) {
                    Promise<Any> { suc, fail ->
                        result.then({ suc(it ?: Unit)}, fail) } as Promise<S>
                } else {
                    Promise { success, _ -> success(result) }
                }

        fun <S> resolve(promise: Promise<S>): Promise<S> = promise
    }
}

fun <T> Promise<Promise<T>>.unwrap() : Promise<T> {
    return Promise(this.cancelMode, {
        resolve, reject, onCancel ->
            onCancel { this.cancel() }
        then({ inner -> inner.then(resolve, reject)
            .cancelled(reject)}, reject)
            .cancelled(reject)
    })
}

private operator fun <R> (() -> R)?.plus(other: (() -> R)) : (() -> R) {
    return if (this == null) other else ({
        this()
        other()
    })
}

private operator fun <T, R> ((T) -> R)?.plus(other: ((T) -> R)) : ((T) -> R) {
    return if (this == null) other else ({
        this(it)
        other(it)
    })
}
