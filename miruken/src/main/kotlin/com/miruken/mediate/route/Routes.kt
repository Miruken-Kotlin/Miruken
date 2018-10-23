package com.miruken.mediate.route

import com.miruken.Stage
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise

class Routes<Res: Any>(
        vararg val schemes: String
) : Filtering<Routed<Res>, Res> {

    override var order: Int? = Stage.LOGGING - 1

    override fun next(
            callback: Routed<Res>,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ): Promise<Res> {
        return next()
    }
}