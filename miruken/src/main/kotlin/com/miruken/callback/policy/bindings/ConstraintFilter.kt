package com.miruken.callback.policy.bindings

import com.miruken.Stage
import com.miruken.callback.*

class ConstraintFilter<Res>: Filtering<BindingScope, Res> {
    override var order: Int? = Stage.FILTER

    override fun next(
            callback: BindingScope,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ) = if (provider is ConstraintProvider &&
            provider.constraint.matches(callback.metadata)) {
            next()
        } else {
            next.abort()
        }
    }