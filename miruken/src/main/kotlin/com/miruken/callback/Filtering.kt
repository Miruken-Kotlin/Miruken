package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.allInterfaces
import kotlin.reflect.KClass

typealias Next<Res> = () -> Res

@FunctionalInterface
interface Filtering<in Cb: Any, Res: Any?> : Ordered {
    fun next(
            callback: Cb,
            binding:  MethodBinding,
            composer: Handling,
            next:     Next<Res>
    ): Res
}

interface GlobalFiltering<in Cb: Any, Res: Any?> : Filtering<Cb, Res>

fun KClass<out Filtering<*,*>>.getFilteringInterface() =
        allInterfaces.single { it.classifier == Filtering::class }
