package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.concurrent.Promise

class AsyncFilterAdapter<in Cb: Any, Res: Any?>(
        val filter: Filtering<Cb, Res>,
        private val timeoutMs: Long? = null
) : Filtering<Cb, Promise<Res>> {

    override var order: Int?
        get() = filter.order
        set(value) { filter.order = value}

    override fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<Promise<Res>>
    ): Promise<Res> {
        @Suppress("UNCHECKED_CAST")
        return Promise.resolve<Any?>(
                filter.next(callback, binding, composer,
                { next().get(timeoutMs) })) as Promise<Res>
    }
}

class SyncFilterAdapter<in Cb: Any, Res: Any?>(
        val filter: Filtering<Cb, Promise<Res>>,
        private val timeoutMs: Long? = null
) : Filtering<Cb, Res> {

    override var order: Int?
        get() = filter.order
        set(value) { filter.order = value}

    override fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<Res>
    ): Res {
        @Suppress("UNCHECKED_CAST")
        return filter.next(callback, binding, composer,
                { Promise.resolve<Any?>(next()) as Promise<Res> })
                .get(timeoutMs)
    }
}