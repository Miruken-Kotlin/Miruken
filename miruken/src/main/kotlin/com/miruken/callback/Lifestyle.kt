package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

abstract class Lifestyle<Res> : Filtering<Inquiry, Res> {
    override var order: Int? = Int.MAX_VALUE - 100

    override fun next(
            callback:    Inquiry,
            rawCallback: Any,
            binding:     MemberBinding,
            composer:    Handling,
            next:        Next<Res>,
            provider:    FilteringProvider?
    ): Promise<Res> {
        val parent = callback.parent
        if (parent == null || isCompatibleWithParent(parent)) {
            val instance = getInstance(callback, binding, next, composer)
            if (instance != null)  return instance
        }
        return next.abort()
    }

    abstract fun isCompatibleWithParent(parent: Inquiry): Boolean

    abstract fun getInstance(
            inquiry:  Inquiry,
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Promise<Res>?
}

abstract class LifestyleProvider : FilteringProvider {
    final override val required = true

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ): List<Lifestyle<*>> {
        val resultType = filterType.arguments[1].type!!
        return listOf(LIFESTYLES.getOrPut(
                binding to resultType, ::createLifestyle))
    }

    abstract fun createLifestyle(): Lifestyle<*>

    override fun equals(other: Any?) =
            this === other || other?.let {
                this::class == it::class
            } ?: false

    override fun hashCode() = this::class.hashCode()
}

private val LIFESTYLES = ConcurrentHashMap<
        Pair<MemberBinding, KType>, Lifestyle<*>>()