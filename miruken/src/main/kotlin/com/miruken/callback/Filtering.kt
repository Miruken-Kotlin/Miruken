package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.allInterfaces
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType

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

fun KClass<out Filtering<*,*>>.getFilteringInterface() =
        if (this == Filtering::class) FILTERING_STAR else
        allInterfaces.single { it.classifier == Filtering::class }

val FILTERING_STAR = Filtering::class.starProjectedType
