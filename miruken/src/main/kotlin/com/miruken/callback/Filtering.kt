package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding

typealias Next<Res> = (Handling?) -> Res

interface Filtering<in Cb, out Res> : Ordered {
    fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<@UnsafeVariance Res>
    ): Res
}

interface GlobalFiltering<in Cb, out Res> : Filtering<Cb, Res>
