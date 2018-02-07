package com.miruken.concurrent

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

open class Promise<out T> private constructor() {
    private var _fulfilled  : ((Any) -> Unit) = {}
    private var _rejected   : ((Throwable) -> Unit) = {}
    private val _completed  : AtomicBoolean = AtomicBoolean()
    private var _throwable  : Throwable? = null
    private var _result     : T? = null
    private var _onCancel   : (() -> Unit) = {}
    private val _childCount : AtomicInteger = AtomicInteger()
    private val _guard      = java.lang.Object()

    constructor(
            executor: ((T) -> Unit, (Throwable) -> Unit) -> Unit
    ) : this(ChildCancelMode.All, executor)

    constructor(
            mode: ChildCancelMode,
            executor: ((T) -> Unit, (Throwable) -> Unit) -> Unit
    ) : this() {
        cancelMode = mode
        try {
            executor(::resolve, ::reject)
        } catch (throwable: Throwable) {
            reject(throwable)
        }
    }

    constructor(
            executor: ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this(ChildCancelMode.All, executor)

    constructor(
            mode: ChildCancelMode,
            executor: ((T) -> Unit, (Throwable) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this() {
        cancelMode = mode
        try {
            executor(::resolve, ::reject) {
                _onCancel.combine(it)
            }
        } catch (throwable: Throwable) {
            reject(throwable)
        }
    }

    var state : PromiseState = PromiseState.Pending
        protected set

    val isCompleted get() = _completed.get()

    var cancelMode: ChildCancelMode = ChildCancelMode.All
        private set

    fun <R> then(then: ((T) -> R)) : Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    resolveChild(then(result as T))
                } catch (ex: Throwable) {
                    rejectChild(ex)
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
                    _fulfilled = _fulfilled.combine(res)
                    _rejected  = _rejected.combine(rejectChild)
                }
            }
        }
    }

    fun <R> then(then: ((T) -> R), fail: ((Throwable) -> R)) : Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    resolveChild(then(result as T))
                } catch (ex: Throwable) {
                    rejectChild(ex)
                    return
                }
            }
            val rej: ((Throwable) -> Unit) = fun(throwable) {
                if (throwable !is CancellationException) {
                    try {
                        resolveChild(fail(throwable))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(throwable)
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
                    _fulfilled = _fulfilled.combine(res)
                    _rejected  = _rejected.combine(rej)
                }
            }
        }
    }

    fun catch(fail: ((Throwable) -> Any)) : Promise<Any> {
        return createChild { resolveChild, rejectChild ->
            val rej: ((Throwable) -> Unit) = fun(throwable) {
                if (throwable !is CancellationException) {
                    try {
                        resolveChild(fail(throwable))
                    } catch (t: Throwable) {
                        rejectChild(t)
                    }
                } else {
                    rejectChild(throwable)
                }
            }
            synchronized (_guard) {
                if (isCompleted)
                {
                    if (state == PromiseState.Fulfilled)
                        resolveChild(_result as Any)
                    else
                        rej(_throwable!!)
                }
                else
                {
                    _fulfilled = _fulfilled.combine(resolveChild)
                    _rejected  = _rejected.combine(rej)
                }
            }
        }
    }

    fun finally(final: (() -> Unit)) : Promise<T> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    final()
                } catch (ex: Throwable) {
                    rejectChild(ex)
                    return
                }
                @Suppress("UNCHECKED_CAST")
                resolveChild(result as T)
            }
            val rej: ((Throwable) -> Unit) = fun(throwable) {
                try {
                    final()
                    rejectChild(throwable)
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
                    _fulfilled = _fulfilled.combine(res)
                    _rejected  = _rejected.combine(rej)
                }
            }
        }
    }

    fun cancel() {
        reject(CancellationException())
    }

    fun cancelled(cancelled: ((CancellationException) -> Unit)) {
        synchronized (_guard) {
            if (isCompleted) {
                if (state == PromiseState.Cancelled) {
                    cancelled(_throwable as CancellationException)
                }
            } else {
                _rejected = _rejected.combine {
                    val cancel = it as? CancellationException
                    if (cancel != null) cancelled(cancel)
                }
            }
        }
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

    private fun reject(throwable: Throwable) {
        if (_completed.compareAndSet(false, true)) {
            _throwable = throwable
            val isCancellation = throwable is CancellationException
            if (isCancellation) {
                try {
                    _onCancel()
                }
                catch (t: Throwable) {
                    // consume errorsß
                }
            }
            synchronized (_guard) {
                state = if (isCancellation) PromiseState.Cancelled
                        else PromiseState.Rejected
                val rejected = _rejected
                _fulfilled = {}
                _rejected  = {}
                rejected(throwable)
            }
        }
    }
}


fun <T> T.toPromise() : Promise<T> {
    return Promise { resolve, _ ->
        resolve(this)
    }
}

fun <T> Promise<T>.toPromise() : Promise<T> {
    return Promise { resolve, reject ->
        then(resolve, reject)
    }
}

fun <T> Promise<Promise<T>>.unwrap() : Promise<T> {
    return Promise(this.cancelMode, {
        resolve, reject, onCancel ->
            onCancel { this.cancel() }
        then({ inner -> inner.then(
                { result -> resolve(result) },
                { throwable -> reject(throwable) }
            ).cancelled(reject)}, reject).cancelled(reject)
    })
}

private fun <R> (() -> R).combine(other: (() -> R)) : (() -> R) {
    return {
        this()
        other()
    }
}

private fun <T, R> ((T) -> R).combine(other: ((T) -> R)) : ((T) -> R) {
    return {
        this(it)
        other(it)
    }
}
