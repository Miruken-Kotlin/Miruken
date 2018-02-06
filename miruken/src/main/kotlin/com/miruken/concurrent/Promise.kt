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

open class Promise<T> private constructor() {
    private var _fulfilled  : ((Any) -> Unit) = {}
    private var _rejected   : ((Exception) -> Unit) = {}
    private val _completed  : AtomicBoolean = AtomicBoolean()
    private val _exception  : Exception? = null
    private val _result     : T? = null
    private var _onCancel   : (() -> Unit) = {}
    private val _childCount : AtomicInteger = AtomicInteger()
    private val _guard      = java.lang.Object()

    constructor(
            owner: ((T) -> Unit, (Exception) -> Unit) -> Unit
    ) : this(ChildCancelMode.All, owner)

    constructor(
            mode: ChildCancelMode,
            owner: ((T) -> Unit, (Exception) -> Unit) -> Unit
    ) : this() {
        cancelMode = mode
        try {
            owner(::resolve, ::reject)
        } catch (exception: Exception) {
            reject(exception)
        }
    }

    constructor(
            owner: ((T) -> Unit, (Exception) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this(ChildCancelMode.All, owner)

    constructor(
            mode: ChildCancelMode,
            owner: ((T) -> Unit, (Exception) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : this() {
        cancelMode = mode
        try {
            owner(::resolve, ::reject) {
                _onCancel.combine(it)
            }
        } catch (exception: Exception) {
            reject(exception)
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
                } catch (ex: Exception) {
                    rejectChild(ex)
                    return
                }
            }
            synchronized (_guard) {
                if (isCompleted)
                {
                    if (state == PromiseState.Fulfilled)
                        res(_result!!)
                    else
                        rejectChild(_exception!!)
                }
                else
                {
                    _fulfilled = _fulfilled.combine(res)
                    _rejected  = _rejected.combine(rejectChild)
                }
            }
        }
    }

    fun <R> then(then: ((T) -> R), fail: ((Exception) -> R)) : Promise<R> {
        return createChild { resolveChild, rejectChild ->
            val res: ((Any) -> Unit) = fun(result) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    resolveChild(then(result as T))
                } catch (ex: Exception) {
                    rejectChild(ex)
                    return
                }
            }
            val rej: ((Exception) -> Unit) = fun(exception) {
                if (exception !is CancellationException) {
                    try {
                        resolveChild(fail(exception))
                    } catch (exo: Exception) {
                        rejectChild(exo)
                    }
                } else {
                    rejectChild(exception)
                }
            }
            synchronized (_guard) {
                if (isCompleted)
                {
                    if (state == PromiseState.Fulfilled)
                        res(_result!!)
                    else
                        rej(_exception!!)
                }
                else
                {
                    _fulfilled = _fulfilled.combine(res)
                    _rejected  = _rejected.combine(rej)
                }
            }
        }
    }

    fun cancel() {

    }

    fun cancelled(cancelled: ((CancellationException) -> Unit)) {
        synchronized (_guard) {
            if (isCompleted) {
                if (state == PromiseState.Cancelled) {
                    cancelled(_exception as CancellationException)
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
            owner: ((R) -> Unit, (Exception) -> Unit) -> Unit
    ): Promise<R> {
        val child = createChild<R>(cancelMode) { resolve, reject, onCancel ->
            owner(resolve, reject)
            onCancel {
                if (cancelMode == ChildCancelMode.Any ||
                        _childCount.decrementAndGet() == 0)
                    cancel()
            }
        }
        if (cancelMode == ChildCancelMode.All)
            _childCount.incrementAndGet()
        return child;
    }

    protected open fun <R> createChild(
        mode:  ChildCancelMode,
        owner: ((R) -> Unit, (Exception) -> Unit, (((() -> Unit)) -> Unit)) -> Unit
    ) : Promise<R>
    {
        return Promise(mode, owner)
    }

    private fun resolve(result: T) {

    }

    private fun reject(exception: Exception) {

    }
}

fun <T> Promise<Promise<T>>.unwrap() : Promise<T> {
    return Promise(this.cancelMode, {
        resolve, reject, onCancel ->
            onCancel { this.cancel() }
        then { inner -> {

        }}
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
