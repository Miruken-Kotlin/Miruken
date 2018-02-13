package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding

interface Filtering<in Cb, Res> : Ordered {
    fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     (Boolean, Handling?) -> Res
    ) : Res
}

interface GlobalFiltering<in Cb, Res> : Filtering<Cb, Res>
