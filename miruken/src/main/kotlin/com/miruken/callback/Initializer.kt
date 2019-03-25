package com.miruken.callback

import com.miruken.Initializing
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.concurrent.flatMap
import kotlin.reflect.KType

class Initializer<Res> : Filtering<Inquiry, Res> {
    override var order: Int? = Int.MAX_VALUE - 100

    override fun next(
            callback:    Inquiry,
            rawCallback: Any,
            binding:     MemberBinding,
            composer:    Handling,
            next:        Next<Res>,
            provider:    FilteringProvider?
    ) = next() flatMap { result ->
        @Suppress("UNCHECKED_CAST")
        (result as? Initializing)?.let {
            it.initialize()?.then { result }
        } ?: Promise.resolve(result as Any) as Promise<Res>
    }
}

object InitializeProvider : FilteringProvider {
    private var filters = listOf(Initializer<Any>())

    override val required = true

    override fun getFilters(
            binding:    MemberBinding,
            filterType: KType,
            composer:   Handling
    ) = filters
}