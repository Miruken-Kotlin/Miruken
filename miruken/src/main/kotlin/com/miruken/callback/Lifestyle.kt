package com.miruken.callback

import com.miruken.callback.policy.MemberBinding
import com.miruken.callback.policy.PolicyMemberBinding
import com.miruken.concurrent.Promise
import com.miruken.runtime.getMetaAnnotations
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

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

    object Validator : FilterProviderValidating {
        override fun validate(
                filterProviderClass: KClass<out FilteringProvider>,
                binding:             MemberBinding
        ) {
            val policyBinding = binding as? PolicyMemberBinding
            if (policyBinding?.policy != ProvidesPolicy) {
                error("${filterProviderClass.simpleName} cannot be applied to '${binding.member}' since the member is not a Provider")
            }

            if (policyBinding.dispatcher
                    .getMetaAnnotations<UseFilterProvider>()
                    .flatMap { it.second }
                    .asSequence()
                    .filter { it.filterProviderClass.isSubclassOf(
                            LifestyleProvider::class)
                    }.count() > 1) {
                error("More than one lifestyle is not allowed for member '${binding.member}'")
            }
        }
    }
}

private val LIFESTYLES = ConcurrentHashMap<
        Pair<MemberBinding, KType>, Lifestyle<*>>()