package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.allInterfaces
import kotlin.reflect.KClass

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

fun getFilterInterface(clazz: KClass<*>) =
        clazz.allInterfaces.singleOrNull{ it.classifier == Filtering::class }
