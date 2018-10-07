package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

abstract class Lifestyle<Res> : Filtering<Inquiry, Res> {
    override var order: Int? = null

    override fun next(
            callback: Inquiry,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ) = getInstance(binding, next, composer)?.let {
        @Suppress("UNCHECKED_CAST")
        Promise.resolve(it as Any) as Promise<Res>
    } ?: next.abort()

    abstract fun getInstance(
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Res?
}

open class LifestyleProvider(
        private val initializer: () -> Lifestyle<*>
): FilteringProvider {
    override val required = true

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Lifestyle<*>> {
        val resultType = filterType.arguments[1].type!!
        return listOf(LIFESTYLES.getOrPut(
                binding to resultType, initializer))
    }
}

private val LIFESTYLES = ConcurrentHashMap<
        Pair<MemberBinding, KType>, Lifestyle<*>>()