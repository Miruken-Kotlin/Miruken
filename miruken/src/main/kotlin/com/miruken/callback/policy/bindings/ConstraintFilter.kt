package com.miruken.callback.policy.bindings

import com.miruken.Stage
import com.miruken.callback.*
import com.miruken.concurrent.Promise

class ConstraintFilter<Res>: Filtering<BindingScope, Res> {
    override var order: Int? = Stage.FILTER

    override fun next(
            callback: BindingScope,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ): Promise<Res> {
        if (provider is ConstraintProvider &&
            provider.constraint.matches(callback.metadata)) {
            return next()
        }
        return next.abort()
    }
}