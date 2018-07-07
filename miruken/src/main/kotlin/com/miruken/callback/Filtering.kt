package com.miruken.callback

import com.miruken.Ordered
import com.miruken.callback.policy.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.runtime.allInterfaces
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType

typealias Next<Res> = (Handling?, Boolean?) -> Promise<Res>

interface Filtering<in Cb: Any, Res: Any?> : Ordered {
    fun next(
            callback: Cb,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider? = null
    ): Promise<Res>
}

operator fun <Res> Next<Res>.invoke(composer: Handling? = null) =
        this(composer, true)

fun <Res> Next<Res>.abort() = this(null, false)

fun KClass<out Filtering<*,*>>.getFilteringInterface() =
        if (this == Filtering::class) FILTERING_STAR else
        allInterfaces.single { it.classifier == Filtering::class }

val FILTERING_STAR = Filtering::class.starProjectedType
