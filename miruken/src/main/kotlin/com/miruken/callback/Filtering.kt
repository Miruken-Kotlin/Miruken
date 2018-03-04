package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding

typealias Next<Res> = (Boolean, Handling?) -> Res

interface Filtering<in Cb, Res> : Ordered {
    fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<Res>
    ): Res
}

interface GlobalFiltering<in Cb, Res> : Filtering<Cb, Res>
