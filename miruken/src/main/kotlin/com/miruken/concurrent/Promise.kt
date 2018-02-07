package com.miruken.concurrent

import com.miruken.*
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class PromiseState {
    Pending,
    Fulfilled,
    Rejected,
    Cancelled
}

enum class ChildCancelMode {
    All,
    Any
}

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

    constructor(
            executor: ((T) -> Unit, (Throwable) -> Unit) -> Unit
    ) : this(ChildCancelMode.All, executor)

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
    ) : this(ChildCancelMode.All, executor)

    constructor(
            mode: ChildCancelMode,
            executor: ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this(mode) {
        try {
            executor(::resolve, ::reject) {
                _onCancel += it
            }
        } catch (e: Throwable) {
            reject(e)
        }
    }

    var state : PromiseState = PromiseState.Pending
        protected set

    private val isCompleted get() = _completed.get()

    fun <R> then(success: ((T) -> R)) : Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    resolveChild(success(result as T))
                } catch (e: Throwable) {
                    rejectChild(e)
                    return
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    if (state == PromiseState.Fulfilled)
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
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    resolveChild(Either.Right(success(result as T)))
                } catch (e: Throwable) {
                    rejectChild(e)
                    return
                }
            }
            val rej: ((Throwable) -> Unit) = fun(e) {
                if (e !is CancellationException) {
                    try {
                        resolveChild(Either.Left(fail(e)))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(e)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    if (state == PromiseState.Fulfilled)
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

    fun <R> catch(fail: ((Throwable) -> R)) : Promise<Either<R, T>> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) =  fun(result) {
                @Suppress("UNCHECKED_CAST")
                resolveChild(Either.Right(result as T))
            }
            val rej: ((Throwable) -> Unit) = fun(e) {
                if (e !is CancellationException) {
                    try {
                        resolveChild(Either.Left(fail(e)))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(e)
                }
            }
            synchronized (_guard) {
                if (isCompleted)
                {
                    if (state == PromiseState.Fulfilled)
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

    fun finally(final: (() -> Unit)) : Promise<T> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    final()
                } catch (e: Throwable) {
                    rejectChild(e)
                    return
                }
                @Suppress("UNCHECKED_CAST")
                resolveChild(result as T)
            }
            val rej: ((Throwable) -> Unit) = fun(e) {
                try {
                    final()
                    rejectChild(e)
                } catch (t: Throwable) {
                    rejectChild(t)
                }
            }
            synchronized (_guard) {
                if (isCompleted) {
                    if (state == PromiseState.Fulfilled)
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

    fun cancelled(cancelled: ((CancellationException) -> Unit)) : Promise<T> {
        synchronized (_guard) {
            if (isCompleted) {
                if (state == PromiseState.Cancelled) {
                    cancelled(_throwable as CancellationException)
                }
            } else {
                _rejected +=  {
                    val cancel = it as? CancellationException
                    if (cancel != null) cancelled(cancel)
                }
            }
        }
        return this
    }

    protected open fun <R> createChild(
            executor: ((R) -> Unit, (Throwable) -> Unit) -> Unit
    ): Promise<R> {
        val child = createChild<R>(cancelMode) { resolve, reject, onCancel ->
            executor(resolve, reject)
            onCancel {
                if (cancelMode == ChildCancelMode.Any ||
                        _childCount.decrementAndGet() == 0)
                    cancel()
            }
        }
        if (cancelMode == ChildCancelMode.All)
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
                state = PromiseState.Fulfilled
                val fulfilled = _fulfilled
                _fulfilled = {}
                _rejected  = {}
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
                state = if (isCancellation) PromiseState.Cancelled
                        else PromiseState.Rejected
                val rejected = _rejected
                _fulfilled = {}
                _rejected  = {}
                rejected(e)
            }
        }
    }

    companion object {
        fun reject(e: Throwable): Promise<Nothing> =
                Promise { _, reject -> reject(e) }

        fun <S> resolve(result: S): Promise<S> =
                Promise { resolve, _ -> resolve(result) }

        fun <S> resolve(promise: Promise<S>): Promise<S> =
                Promise { resolve, reject ->
                    promise.then(resolve, reject)
                }
    }
}

fun <T> Promise<Promise<T>>.unwrap() : Promise<T> {
    return Promise(this.cancelMode, {
        resolve, reject, onCancel -> onCancel { this.cancel() }
        then({ inner -> inner.then(
                { result -> resolve(result) },
                { e -> reject(e) }
            ).cancelled(reject)}, reject).cancelled(reject)
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
